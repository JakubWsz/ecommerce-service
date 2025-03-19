package pl.ecommerce.product.read.infrastructure.projector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.product.*;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import pl.ecommerce.product.read.infrastructure.repository.CategoryReadRepository;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static pl.ecommerce.product.read.infrastructure.projector.ProductEventProjectorHelper.*;

@Component
@Slf4j
public class ProductEventProjector extends DomainEventHandler {

	private final ProductReadRepository productRepository;
	private final CategoryReadRepository categoryRepository;

	public ProductEventProjector(ProductReadRepository productRepository,
								 ObjectMapper objectMapper, TopicsProvider topicsProvider,
								 CategoryReadRepository categoryRepository) {
		super(objectMapper, topicsProvider);
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	@EventHandler
	public void on(ProductPriceUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductPriceUpdatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		String spanId = event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null;

		productRepository.updatePrice(
						event.getAggregateId(),
						event.getPrice(),
						event.getDiscountedPrice(),
						traceId,
						spanId)
				.doOnSuccess(result -> log.debug("Updated product price in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating product price in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductStockUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductStockUpdatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		String spanId = event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null;

		productRepository.updateStock(
						event.getAggregateId(),
						event.getQuantity(),
						event.getWarehouseId(),
						traceId,
						spanId)
				.doOnSuccess(result -> log.debug("Updated product stock in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating product stock in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductReservedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductReservedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					// Update reserved quantity
					ProductReadModel.StockInfo stock = product.getStock();
					if (stock == null) {
						stock = new ProductReadModel.StockInfo(0, 0, "DEFAULT");
					}

					stock.setReserved(stock.getReserved() + event.getQuantity());
					product.setStock(stock);

					// Update tracking info
					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null);
					product.setLastOperation("ReserveStock");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Updated product reservation in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating product reservation in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductReservationConfirmedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductReservationConfirmedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					// Update stock and reservation quantities
					ProductReadModel.StockInfo stock = product.getStock();
					if (stock == null) {
						stock = new ProductReadModel.StockInfo(0, 0, "DEFAULT");
					}

					int confirmedQuantity = event.getQuantity();
					stock.setReserved(Math.max(0, stock.getReserved() - confirmedQuantity));
					stock.setAvailable(Math.max(0, stock.getAvailable() - confirmedQuantity));
					product.setStock(stock);

					// Update tracking info
					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null);
					product.setLastOperation("ConfirmReservation");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Confirmed product reservation in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error confirming product reservation in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductReservationReleasedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductReservationReleasedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					// Reset reserved quantity
					ProductReadModel.StockInfo stock = product.getStock();
					if (stock == null) {
						stock = new ProductReadModel.StockInfo(0, 0, "DEFAULT");
					}

					stock.setReserved(0);
					product.setStock(stock);

					// Update tracking info
					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null);
					product.setLastOperation("ReleaseReservation");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Released product reservation in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error releasing product reservation in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductVariantAddedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductVariantAddedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					// Initialize variants list if needed
					if (product.getVariants() == null) {
						product.setVariants(new ArrayList<>());
					}

					// Create new variant
					ProductReadModel.ProductVariant variant = new ProductReadModel.ProductVariant();
					variant.setId(event.getVariantId());
					variant.setSku(event.getSku());
					variant.setAttributes(event.getAttributes().stream()
							.map(attr -> new ProductReadModel.ProductAttribute(
									attr.getName(), attr.getValue(), attr.getUnit()))
							.collect(java.util.stream.Collectors.toList()));

					// Set variant price
					ProductReadModel.PriceInfo price = new ProductReadModel.PriceInfo();
					price.setRegular(event.getPrice());
					price.setCurrency("USD"); // Default currency
					variant.setPrice(price);

					// Set variant stock
					ProductReadModel.StockInfo stock = new ProductReadModel.StockInfo();
					stock.setAvailable(event.getStock());
					stock.setReserved(0);
					stock.setWarehouseId("DEFAULT"); // Default warehouse
					variant.setStock(stock);

					// Add variant to product
					product.getVariants().add(variant);

					// Update tracking info
					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null);
					product.setLastOperation("AddVariant");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Added product variant to read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error adding product variant to read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductCreatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductCreatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		ProductReadModel product = buildProductReadModel(event, traceId);

		// Aktualizacja liczników produktów w kategoriach - zbiorczo
		Set<UUID> categoryIds = product.getCategoryIds();

		productRepository.save(product)
				.doOnSuccess(saved -> {
					log.debug("Product read model saved successfully: {}, traceId: {}",
							saved.getId(), traceId);

					// Zrobimy jedną operację zbiorcza dla wszystkich kategorii
					batchUpdateCategoryProductCounts(categoryIds, traceId);
				})
				.doOnError(error -> log.error("Error saving product read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductUpdatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		// Aktualizacja liczników produktów, jeśli zmieniono kategorie
		if (event.getChanges().containsKey("categories")) {
			productRepository.findById(event.getAggregateId())
					.flatMap(product -> {
						Set<UUID> oldCategories = product.getCategoryIds() != null ? product.getCategoryIds() : new HashSet<>();
						@SuppressWarnings("unchecked")
						Set<UUID> newCategories = (Set<UUID>) event.getChanges().get("categories");

						// Zróbmy aktualizację zliczania produktów bardziej atomowo
						if (!oldCategories.equals(newCategories)) {
							// Zbieramy kategorie, które zostały usunięte
							Set<UUID> removedCategories = new HashSet<>(oldCategories);
							removedCategories.removeAll(newCategories);

							// Zbieramy kategorie, które zostały dodane
							Set<UUID> addedCategories = new HashSet<>(newCategories);
							addedCategories.removeAll(oldCategories);

							// Aktualizujemy liczniki w usuwanych kategoriach
							batchUpdateCategoryProductCounts(removedCategories, -1, traceId);

							// Aktualizujemy liczniki w dodawanych kategoriach
							batchUpdateCategoryProductCounts(addedCategories, 1, traceId);
						}

						return Mono.just(product);
					})
					.subscribe();
		}

		Update update = buildUpdateForEvent(event, traceId);
		productRepository.updateProduct(event.getAggregateId(), update, traceId)
				.doOnSuccess(result -> log.debug("Updated product read model: {}, modified: {}, traceId: {}",
						event.getAggregateId(), result.getModifiedCount(), traceId))
				.doOnError(error -> log.error("Error updating product read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(ProductDeletedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductDeletedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		String spanId = event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null;

		// Pobierz produkt, aby uzyskać jego kategorie przed usunięciem
		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					// Jednoczesna aktualizacja wszystkich liczników dla kategorii
					batchUpdateCategoryProductCounts(product.getCategoryIds(), -1, traceId);

					return productRepository.markAsDeleted(event.getAggregateId(), traceId, spanId);
				})
				.doOnSuccess(result -> log.debug("Marked product as deleted in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error marking product as deleted in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	/**
	 * Jednoczesna aktualizacja licznika produktów dla wielu kategorii.
	 * Zamiast robić osobne wywołania dla każdej kategorii, zbieramy je i przetwarzamy razem.
	 */
	private void batchUpdateCategoryProductCounts(Set<UUID> categoryIds, String traceId) {
		batchUpdateCategoryProductCounts(categoryIds, 1, traceId);
	}

	/**
	 * Jednoczesna aktualizacja licznika produktów dla wielu kategorii z określonym przyrostem.
	 */
	private void batchUpdateCategoryProductCounts(Set<UUID> categoryIds, int delta, String traceId) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return;
		}

		log.debug("Batch updating product count for {} categories, delta: {}, traceId: {}",
				categoryIds.size(), delta, traceId);

		// Zbiór kategorii nadrzędnych, do których także trzeba zaktualizować liczniki
		Set<UUID> parentCategoriesToUpdate = new HashSet<>();

		// Jednoczesna aktualizacja wszystkich kategorii
		Mono.just(categoryIds)
				.flatMapMany(ids -> {
					// Dla każdej kategorii zbieramy także jej rodziców
					return Flux.fromIterable(ids)
							.flatMap(categoryId ->
									// Najpierw aktualizujemy bieżącą kategorię
									categoryRepository.incrementProductCount(categoryId, delta, traceId)
											.doOnSuccess(result -> log.debug("Updated product count for category: {}, delta: {}, modified: {}, traceId: {}",
													categoryId, delta, result.getModifiedCount(), traceId))
											.then(
													// Następnie znajdujemy kategorie nadrzędne
													categoryRepository.findById(categoryId)
															.flatMap(category -> {
																if (category.getParentCategoryId() != null) {
																	// Dodajemy kategorię nadrzędną do zbioru do zaktualizowania
																	parentCategoriesToUpdate.add(category.getParentCategoryId());
																}
																return Mono.empty();
															})
											)
							)
							// Po przetworzeniu wszystkich kategorii, zbieramy unikalne kategorie nadrzędne
							.thenMany(Flux.fromIterable(parentCategoriesToUpdate))
							// I rekurencyjnie aktualizujemy liczniki dla kategorii nadrzędnych
							.flatMap(parentId ->
									categoryRepository.incrementProductCount(parentId, delta, traceId)
											.doOnSuccess(result -> log.debug("Updated product count for parent category: {}, delta: {}, modified: {}, traceId: {}",
													parentId, delta, result.getModifiedCount(), traceId))
							);
				})
				.doOnError(error -> log.error("Error batch updating category product counts: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}
}