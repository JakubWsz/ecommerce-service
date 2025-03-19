package pl.ecommerce.product.read.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.StockVerificationItem;
import pl.ecommerce.product.read.api.dto.StockVerificationRequest;
import pl.ecommerce.product.read.api.dto.StockVerificationResponse;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockVerificationService {

	private final ProductReadRepository productRepository;

	/**
	 * Weryfikuje dostępność produktów dla zamówienia
	 */
	public Mono<StockVerificationResponse> verifyStock(StockVerificationRequest request, TracingContext tracingContext) {
		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		log.debug("Verifying stock for {} items, traceId: {}",
				request.getItems().size(), traceId);

		// Lista pozycji o niewystarczającym stanie magazynowym
		List<StockVerificationItem> insufficientItems = new ArrayList<>();

		// Pobieramy i weryfikujemy każdy produkt
		return Flux.fromIterable(request.getItems())
				.flatMap(item -> {
					UUID productId = item.getProductId();
					int requestedQuantity = item.getQuantity();

					return productRepository.findById(productId)
							.map(product -> {
								int availableQuantity = calculateAvailableQuantity(product);
								boolean isAvailable = availableQuantity >= requestedQuantity;

								if (!isAvailable) {
									// Dodaj do listy niedostępnych
									StockVerificationItem insufficientItem =
											new StockVerificationItem(productId, requestedQuantity, availableQuantity);
									insufficientItems.add(insufficientItem);
								}

								return isAvailable;
							})
							.defaultIfEmpty(false) // Produkt nie istnieje
							.doOnNext(isAvailable -> {
								if (!isAvailable) {
									log.debug("Product unavailable or insufficient stock: {}, requested: {}, traceId: {}",
											productId, requestedQuantity, traceId);
								}
							});
				})
				.collectList()
				.map(results -> {
					boolean allAvailable = results.stream().allMatch(available -> available);

					return StockVerificationResponse.builder()
							.success(allAvailable)
							.insufficientItems(insufficientItems)
							.traceId(traceId)
							.build();
				});
	}

	/**
	 * Sprawdza czy pojedynczy produkt jest dostępny w żądanej ilości
	 */
	public Mono<Boolean> isProductAvailable(UUID productId, int quantity, TracingContext tracingContext) {
		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		log.debug("Checking availability of product: {}, quantity: {}, traceId: {}",
				productId, quantity, traceId);

		return productRepository.findById(productId)
				.map(product -> {
					int availableQuantity = calculateAvailableQuantity(product);
					boolean isAvailable = availableQuantity >= quantity;

					log.debug("Product {}, available: {}, requested: {}, isAvailable: {}, traceId: {}",
							productId, availableQuantity, quantity, isAvailable, traceId);

					return isAvailable;
				})
				.defaultIfEmpty(false); // Produkt nie istnieje
	}

	/**
	 * Oblicza faktycznie dostępną ilość produktu
	 * (ilość w magazynie minus rezerwacje)
	 */
	private int calculateAvailableQuantity(ProductReadModel product) {
		if (Objects.isNull(product) || Objects.isNull(product.getStock())) {
			return 0;
		}

		return Math.max(0, product.getStock().getAvailable() - product.getStock().getReserved());
	}
}

