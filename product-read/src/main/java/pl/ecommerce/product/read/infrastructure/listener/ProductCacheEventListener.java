package pl.ecommerce.product.read.infrastructure.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.product.ProductCreatedEvent;
import pl.ecommerce.commons.event.product.ProductDeletedEvent;
import pl.ecommerce.commons.event.product.ProductUpdatedEvent;
import pl.ecommerce.product.read.application.service.ProductCacheService;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheEventListener {

	private final ProductCacheService cacheService;
	private final ProductReadRepository productRepository;

	@EventListener
	public void handleProductCreatedEvent(ProductCreatedEvent event) {
		log.debug("Handling ProductCreatedEvent for cache: {}", event.getProductId());

		productRepository.findById(event.getProductId())
				.flatMap(cacheService::cacheProduct)
				.subscribe(
						result -> log.debug("Product cached: {}, success: {}", event.getProductId(), result),
						error -> log.error("Error caching product: {}", error.getMessage())
				);
	}

	@EventListener
	public void handleProductUpdatedEvent(ProductUpdatedEvent event) {
		log.debug("Handling ProductUpdatedEvent for cache: {}", event.getAggregateId());

		// Odśwież cache
		cacheService.removeProduct(event.getAggregateId())
				.then(productRepository.findById(event.getAggregateId()))
				.flatMap(cacheService::cacheProduct)
				.subscribe(
						result -> log.debug("Product cache updated: {}, success: {}", event.getAggregateId(), result),
						error -> log.error("Error updating product in cache: {}", error.getMessage())
				);
	}

	@EventListener
	public void handleProductStockUpdatedEvent(ProductStockUpdatedEvent event) {
		log.debug("Handling ProductStockUpdatedEvent for cache: {}", event.getAggregateId());

		cacheService.updateProductStock(event.getAggregateId(), event.getQuantity(), 0)
				.subscribe(
						result -> log.debug("Product stock updated in cache: {}, success: {}", event.getAggregateId(), result),
						error -> log.error("Error updating product stock in cache: {}", error.getMessage())
				);
	}

	@EventListener
	public void handleProductReservedEvent(ProductReservedEvent event) {
		log.debug("Handling ProductReservedEvent for cache: {}", event.getAggregateId());

		// Pobierz aktualny produkt, aby uzyskać dostępną ilość
		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					int available = product.getStock().getAvailable();
					int reserved = product.getStock().getReserved();
					return cacheService.updateProductStock(event.getAggregateId(), available, reserved);
				})
				.subscribe(
						result -> log.debug("Product reservation reflected in cache: {}, success: {}",
								event.getAggregateId(), result),
						error -> log.error("Error updating product reservation in cache: {}", error.getMessage())
				);
	}

	@EventListener
	public void handleProductReservationConfirmedEvent(ProductReservationConfirmedEvent event) {
		log.debug("Handling ProductReservationConfirmedEvent for cache: {}", event.getAggregateId());

		// Po potwierdzeniu rezerwacji należy usunąć produkt z cache jeśli brak zapasów
		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					int available = product.getStock().getAvailable();
					int reserved = product.getStock().getReserved();

					if (available <= 0) {
						return cacheService.removeProduct(event.getAggregateId());
					} else {
						return cacheService.updateProductStock(event.getAggregateId(), available, reserved);
					}
				})
				.subscribe(
						result -> log.debug("Product reservation confirmation reflected in cache: {}, success: {}",
								event.getAggregateId(), result),
						error -> log.error("Error handling reservation confirmation in cache: {}", error.getMessage())
				);
	}

	@EventListener
	public void handleProductReservationReleasedEvent(ProductReservationReleasedEvent event) {
		log.debug("Handling ProductReservationReleasedEvent for cache: {}", event.getAggregateId());

		// Po zwolnieniu rezerwacji odświeżamy cache
		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					int available = product.getStock().getAvailable();
					int reserved = product.getStock().getReserved();
					return cacheService.updateProductStock(event.getAggregateId(), available, reserved);
				})
				.subscribe(
						result -> log.debug("Product reservation release reflected in cache: {}, success: {}",
								event.getAggregateId(), result),
						error -> log.error("Error handling reservation release in cache: {}", error.getMessage())
				);
	}

	@EventListener
	public void handleProductDeletedEvent(ProductDeletedEvent event) {
		log.debug("Handling ProductDeletedEvent for cache: {}", event.getAggregateId());

		cacheService.removeProduct(event.getAggregateId())
				.subscribe(
						result -> log.debug("Product removed from cache: {}, success: {}", event.getAggregateId(), result),
						error -> log.error("Error removing product from cache: {}", error.getMessage())
				);
	}
}
