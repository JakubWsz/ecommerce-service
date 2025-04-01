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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockVerificationService {

	private final ProductReadRepository productRepository;

	public Mono<StockVerificationResponse> verifyStock(StockVerificationRequest request, TracingContext tracingContext) {
		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		log.debug("Verifying stock for {} items, traceId: {}",
				request.getItems().size(), traceId);

		List<StockVerificationItem> insufficientItems = new ArrayList<>();

		return Flux.fromIterable(request.getItems())
				.flatMap(item -> {
					UUID productId = item.getProductId();
					int requestedQuantity = item.getQuantity();

					return productRepository.findById(productId)
							.map(product -> {
								int availableQuantity = calculateAvailableQuantity(product);
								boolean isAvailable = availableQuantity >= requestedQuantity;

								if (!isAvailable) {
									StockVerificationItem insufficientItem =
											new StockVerificationItem(productId, requestedQuantity, availableQuantity);
									insufficientItems.add(insufficientItem);
								}

								return isAvailable;
							})
							.defaultIfEmpty(false)
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
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> reserveProductStock(UUID productId, int quantity, String reservationId, TracingContext tracingContext) {
		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		String spanId = tracingContext != null ? tracingContext.getSpanId() : null;

		log.debug("Reserving stock for product: {}, quantity: {}, reservationId: {}, traceId: {}",
				productId, quantity, reservationId, traceId);

		return isProductAvailable(productId, quantity, tracingContext)
				.flatMap(isAvailable -> {
					if (!isAvailable) {
						log.debug("Cannot reserve - product unavailable or insufficient stock: {}, quantity: {}, traceId: {}",
								productId, quantity, traceId);
						return Mono.just(false);
					}

					return productRepository.findById(productId)
							.flatMap(product -> {
								ProductReadModel.StockInfo stock = product.getStock();
								if (stock == null) {
									stock = new ProductReadModel.StockInfo(0, 0, "DEFAULT");
									product.setStock(stock);
								}

								stock.setReserved(stock.getReserved() + quantity);

								product.setLastTraceId(traceId);
								product.setLastSpanId(spanId);
								product.setLastOperation("ReserveStock:" + reservationId);
								product.setLastUpdatedAt(Instant.now());

								return productRepository.save(product)
										.thenReturn(true);
							});
				});
	}

	public Mono<Boolean> releaseReservation(UUID productId, String reservationId, TracingContext tracingContext) {
		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		String spanId = tracingContext != null ? tracingContext.getSpanId() : null;

		log.debug("Releasing reservation for product: {}, reservationId: {}, traceId: {}",
				productId, reservationId, traceId);

		return productRepository.findById(productId)
				.flatMap(product -> {
					ProductReadModel.StockInfo stock = product.getStock();
					if (stock == null) {
						log.debug("Cannot release reservation - product has no stock info: {}, traceId: {}",
								productId, traceId);
						return Mono.just(false);
					}

					stock.setReserved(0);

					product.setLastTraceId(traceId);
					product.setLastSpanId(spanId);
					product.setLastOperation("ReleaseReservation:" + reservationId);
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product)
							.thenReturn(true);
				})
				.defaultIfEmpty(false);
	}

	private int calculateAvailableQuantity(ProductReadModel product) {
		if (Objects.isNull(product) || Objects.isNull(product.getStock())) {
			return 0;
		}
		return Math.max(0, product.getStock().getAvailable() - product.getStock().getReserved());
	}
}