package pl.ecommerce.product.read.infrastructure.projector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.product.*;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.product.read.application.service.ProductCacheService;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import pl.ecommerce.product.read.infrastructure.repository.CategoryReadRepository;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static pl.ecommerce.product.read.infrastructure.projector.ProductEventProjectorHelper.*;

@Component
@Slf4j
public class ProductEventProjector extends DomainEventHandler {

	private final ProductReadRepository productRepository;
	private final CategoryReadRepository categoryRepository;
	private final ProductCacheService cacheService;

	public ProductEventProjector(ProductReadRepository productRepository,
								 ObjectMapper objectMapper,
								 TopicsProvider topicsProvider,
								 CategoryReadRepository categoryRepository,
								 ProductCacheService cacheService) {
		super(objectMapper, topicsProvider);
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.cacheService = cacheService;
	}

	@EventHandler
	public void on(ProductPriceUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductPriceUpdatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		String spanId = Objects.isNull(event.getTracingContext()) ? null : event.getTracingContext().getSpanId();

		productRepository.updatePrice(
						event.getAggregateId(),
						event.getPrice(),
						event.getDiscountedPrice(),
						event.getCurrency(),
						traceId,
						spanId)
				.doOnSuccess(result -> log.debug("Updated product price in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating product price in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.flatMap(result -> {
					return productRepository.findById(event.getAggregateId())
							.flatMap(cacheService::cacheProduct)
							.doOnSuccess(cacheResult -> log.debug("Product cache updated after price change: {}, success: {}",
									event.getAggregateId(), cacheResult))
							.doOnError(error -> log.error("Error updating product in cache after price change: {}",
									error.getMessage()));
				})
				.subscribe();
	}

	@EventHandler
	public void on(ProductStockUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductStockUpdatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		String spanId = Objects.isNull(event.getTracingContext()) ? null : event.getTracingContext().getSpanId();

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
				.flatMap(result -> {
					return cacheService.updateProductStock(event.getAggregateId(), event.getQuantity(), 0)
							.doOnSuccess(cacheResult -> log.debug("Product stock updated in cache: {}, success: {}",
									event.getAggregateId(), cacheResult))
							.doOnError(error -> log.error("Error updating product stock in cache: {}",
									error.getMessage()));
				})
				.subscribe();
	}

	@EventHandler
	public void on(ProductReservedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductReservedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					ProductReadModel.StockInfo stock = product.getStock();
					if (Objects.isNull(stock)) {
						stock = new ProductReadModel.StockInfo(0, 0, "DEFAULT");
					}

					stock.setReserved(stock.getReserved() + event.getQuantity());
					product.setStock(stock);

					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(Objects.isNull(event.getTracingContext()) ? null : event.getTracingContext().getSpanId());
					product.setLastOperation("ReserveStock");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Updated product reservation in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating product reservation in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.flatMap(product -> {
					int available = product.getStock().getAvailable();
					int reserved = product.getStock().getReserved();
					return cacheService.updateProductStock(event.getAggregateId(), available, reserved)
							.doOnSuccess(result -> log.debug("Product reservation reflected in cache: {}, success: {}",
									event.getAggregateId(), result))
							.doOnError(error -> log.error("Error updating product reservation in cache: {}",
									error.getMessage()));
				})
				.subscribe();
	}

	@EventHandler
	public void on(ProductReservationConfirmedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductReservationConfirmedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					ProductReadModel.StockInfo stock = product.getStock();
					if (Objects.isNull(stock)) {
						stock = new ProductReadModel.StockInfo(0, 0, "DEFAULT");
					}

					int confirmedQuantity = event.getQuantity();
					stock.setReserved(Math.max(0, stock.getReserved() - confirmedQuantity));
					stock.setAvailable(Math.max(0, stock.getAvailable() - confirmedQuantity));
					product.setStock(stock);

					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(Objects.isNull(event.getTracingContext()) ? null : event.getTracingContext().getSpanId());
					product.setLastOperation("ConfirmReservation");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Confirmed product reservation in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error confirming product reservation in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.flatMap(product -> {
					int available = product.getStock().getAvailable();
					int reserved = product.getStock().getReserved();

					if (available <= 0) {
						return cacheService.removeProduct(event.getAggregateId())
								.doOnSuccess(result -> log.debug("Product removed from cache (no stock): {}, success: {}",
										event.getAggregateId(), result))
								.doOnError(error -> log.error("Error removing product from cache: {}",
										error.getMessage()));
					} else {
						return cacheService.updateProductStock(event.getAggregateId(), available, reserved)
								.doOnSuccess(result -> log.debug("Product reservation confirmation reflected in cache: {}, success: {}",
										event.getAggregateId(), result))
								.doOnError(error -> log.error("Error handling reservation confirmation in cache: {}",
										error.getMessage()));
					}
				})
				.subscribe();
	}

	@EventHandler
	public void on(ProductReservationReleasedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductReservationReleasedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					ProductReadModel.StockInfo stock = product.getStock();
					if (Objects.isNull(stock)) {
						stock = new ProductReadModel.StockInfo(0, 0, "DEFAULT");
					}

					stock.setReserved(0);
					product.setStock(stock);

					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(Objects.isNull(event.getTracingContext()) ? null : event.getTracingContext().getSpanId());
					product.setLastOperation("ReleaseReservation");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Released product reservation in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error releasing product reservation in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.flatMap(product -> {
					int available = product.getStock().getAvailable();
					int reserved = product.getStock().getReserved();
					return cacheService.updateProductStock(event.getAggregateId(), available, reserved)
							.doOnSuccess(result -> log.debug("Product reservation release reflected in cache: {}, success: {}",
									event.getAggregateId(), result))
							.doOnError(error -> log.error("Error handling reservation release in cache: {}",
									error.getMessage()));
				})
				.subscribe();
	}

	@EventHandler
	public void on(ProductVariantAddedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductVariantAddedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					if (Objects.isNull(product.getVariants())) {
						product.setVariants(new ArrayList<>());
					}

					ProductReadModel.ProductVariant variant = new ProductReadModel.ProductVariant();
					variant.setId(event.getVariantId());
					variant.setSku(event.getSku());
					variant.setAttributes(event.getAttributes().stream()
							.map(attr -> new ProductReadModel.ProductAttribute(
									attr.getName(), attr.getValue(), attr.getUnit()))
							.collect(java.util.stream.Collectors.toList()));

					ProductReadModel.PriceInfo price = new ProductReadModel.PriceInfo();
					price.setRegular(event.getPrice());
					price.setCurrency("USD");
					variant.setPrice(price);

					ProductReadModel.StockInfo stock = new ProductReadModel.StockInfo();
					stock.setAvailable(event.getStock());
					stock.setReserved(0);
					stock.setWarehouseId("DEFAULT");
					variant.setStock(stock);

					product.getVariants().add(variant);

					product.setUpdatedAt(event.getTimestamp());
					product.setLastTraceId(traceId);
					product.setLastSpanId(Objects.isNull(event.getTracingContext()) ? null : event.getTracingContext().getSpanId());
					product.setLastOperation("AddVariant");
					product.setLastUpdatedAt(Instant.now());

					return productRepository.save(product);
				})
				.doOnSuccess(saved -> log.debug("Added product variant to read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error adding product variant to read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.flatMap(product -> cacheService.cacheProduct(product)
						.doOnSuccess(result -> log.debug("Product cache updated after variant addition: {}, success: {}",
								event.getAggregateId(), result))
						.doOnError(error -> log.error("Error updating product in cache after variant addition: {}",
								error.getMessage())))
				.subscribe();
	}

	@EventHandler
	public void on(ProductCreatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductCreatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		ProductReadModel product = buildProductReadModel(event, traceId);

		Set<UUID> categoryIds = product.getCategoryIds();

		productRepository.save(product)
				.doOnSuccess(saved -> {
					log.debug("Product read model saved successfully: {}, traceId: {}",
							saved.getId(), traceId);

					batchUpdateCategoryProductCounts(categoryIds, traceId);
				})
				.doOnError(error -> log.error("Error saving product read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.flatMap(saved -> {
					return cacheService.cacheProduct(saved)
							.doOnSuccess(result -> log.debug("Product cached: {}, success: {}",
									event.getProductId(), result))
							.doOnError(error -> log.error("Error caching product: {}",
									error.getMessage()));
				})
				.subscribe();
	}

	@EventHandler
	public void on(ProductUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductUpdatedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		if (event.getChangedFields().containsKey("categories")) {
			productRepository.findById(event.getAggregateId())
					.flatMap(product -> {
						Set<UUID> oldCategories = Objects.nonNull(product.getCategoryIds()) ? product.getCategoryIds() : new HashSet<>();
						@SuppressWarnings("unchecked")
						Set<UUID> newCategories = (Set<UUID>) event.getChangedFields().get("categories");

						if (!oldCategories.equals(newCategories)) {
							Set<UUID> removedCategories = new HashSet<>(oldCategories);
							removedCategories.removeAll(newCategories);

							Set<UUID> addedCategories = new HashSet<>(newCategories);
							addedCategories.removeAll(oldCategories);

							batchUpdateCategoryProductCounts(removedCategories, -1, traceId);

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
				.flatMap(result -> {
					return cacheService.removeProduct(event.getAggregateId())
							.then(productRepository.findById(event.getAggregateId()))
							.flatMap(cacheService::cacheProduct)
							.doOnSuccess(cacheResult -> log.debug("Product cache updated: {}, success: {}",
									event.getAggregateId(), cacheResult))
							.doOnError(error -> log.error("Error updating product in cache: {}",
									error.getMessage()));
				})
				.subscribe();
	}

	@EventHandler
	public void on(ProductDeletedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting ProductDeletedEvent for product: {}, traceId: {}",
				event.getAggregateId(), traceId);

		String spanId = Objects.isNull(event.getTracingContext()) ? null : event.getTracingContext().getSpanId();

		productRepository.findById(event.getAggregateId())
				.flatMap(product -> {
					batchUpdateCategoryProductCounts(product.getCategoryIds(), -1, traceId);

					return productRepository.markAsDeleted(event.getAggregateId(), traceId, spanId);
				})
				.doOnSuccess(result -> log.debug("Marked product as deleted in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error marking product as deleted in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.flatMap(result -> {
					return cacheService.removeProduct(event.getAggregateId())
							.doOnSuccess(cacheResult -> log.debug("Product removed from cache: {}, success: {}",
									event.getAggregateId(), cacheResult))
							.doOnError(error -> log.error("Error removing product from cache: {}",
									error.getMessage()));
				})
				.subscribe();
	}

	private void batchUpdateCategoryProductCounts(Set<UUID> categoryIds, String traceId) {
		batchUpdateCategoryProductCounts(categoryIds, 1, traceId);
	}

	private void batchUpdateCategoryProductCounts(Set<UUID> categoryIds, int delta, String traceId) {
		if (Objects.isNull(categoryIds) || categoryIds.isEmpty()) {
			return;
		}

		log.debug("Batch updating product count for {} categories, delta: {}, traceId: {}",
				categoryIds.size(), delta, traceId);

		Set<UUID> parentCategoriesToUpdate = new HashSet<>();

		Mono.just(categoryIds)
				.flatMapMany(ids -> Flux.fromIterable(ids)
						.flatMap(categoryId ->
								categoryRepository.incrementProductCount(categoryId, delta, traceId)
										.doOnSuccess(result -> log.debug("Updated product count for category: {}, delta: {}, modified: {}, traceId: {}",
												categoryId, delta, result.getModifiedCount(), traceId))
										.then(
												categoryRepository.findById(categoryId)
														.flatMap(category -> {
															if (Objects.nonNull(category.getParentCategoryId())) {
																parentCategoriesToUpdate.add(category.getParentCategoryId());
															}
															return Mono.empty();
														})
										)
						)
						.thenMany(Flux.fromIterable(parentCategoriesToUpdate))
						.flatMap(parentId ->
								categoryRepository.incrementProductCount(parentId, delta, traceId)
										.doOnSuccess(result -> log.debug("Updated product count for parent category: {}, delta: {}, modified: {}, traceId: {}",
												parentId, delta, result.getModifiedCount(), traceId))
						))
				.doOnError(error -> log.error("Error batch updating category product counts: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}
}