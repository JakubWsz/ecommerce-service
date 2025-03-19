package pl.ecommerce.product.read.infrastructure.repository;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.application.mapper.ProductMapper;
import pl.ecommerce.product.read.application.service.ProductCacheService;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ProductReadRepositoryImpl implements ProductReadRepositoryCustom {

	private final ReactiveMongoTemplate mongoTemplate;
	private final ProductCacheService cacheService;

	@Override
	public Mono<Page<ProductSummary>> searchProducts(String query, Pageable pageable, String traceId) {
		log.debug("Searching products with query: {}, traceId: {}", query, traceId);

		String searchTerm = ".*" + query + ".*";

		Criteria criteria = new Criteria().orOperator(
				Criteria.where("name").regex(searchTerm, "i"),
				Criteria.where("description").regex(searchTerm, "i"),
				Criteria.where("sku").regex(searchTerm, "i"),
				Criteria.where("brandName").regex(searchTerm, "i")
		);

		Query searchQuery = new Query(criteria);

		return executePagedQuery(searchQuery, pageable, traceId);
	}

	@Override
	public Mono<Page<ProductSummary>> findByCategory(UUID categoryId, Pageable pageable, String traceId) {
		log.debug("Finding products by category: {}, traceId: {}", categoryId, traceId);

		Criteria criteria = Criteria.where("categoryIds").is(categoryId)
				.and("status").is("ACTIVE");

		Query categoryQuery = new Query(criteria);

		return executePagedQuery(categoryQuery, pageable, traceId);
	}

	@Override
	public Mono<Page<ProductSummary>> findByVendor(UUID vendorId, Pageable pageable, String traceId) {
		log.debug("Finding products by vendor: {}, traceId: {}", vendorId, traceId);

		Criteria criteria = Criteria.where("vendorId").is(vendorId)
				.and("status").is("ACTIVE");

		Query vendorQuery = new Query(criteria);

		return executePagedQuery(vendorQuery, pageable, traceId);
	}

	@Override
	public Mono<Page<ProductSummary>> findFeaturedProducts(Pageable pageable, String traceId) {
		log.debug("Finding featured products, traceId: {}", traceId);

		Criteria criteria = Criteria.where("featured").is(true)
				.and("status").is("ACTIVE");

		Query featuredQuery = new Query(criteria);

		return executePagedQuery(featuredQuery, pageable, traceId);
	}

	@Override
	public Mono<Page<ProductSummary>> filterProducts(
			Set<UUID> categories,
			Set<UUID> vendors,
			UUID brandId,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean inStock,
			Pageable pageable,
			String traceId) {

		log.debug("Filtering products, traceId: {}", traceId);

		Criteria criteria = Criteria.where("status").is("ACTIVE");

		if (Objects.nonNull(categories) && !categories.isEmpty()) {
			criteria = criteria.and("categoryIds").in(categories);
		}

		if (Objects.nonNull(vendors) && !vendors.isEmpty()) {
			criteria = criteria.and("vendorId").in(vendors);
		}

		if (Objects.nonNull(brandId)) {
			criteria = criteria.and("brandId").is(brandId);
		}

		if (Objects.nonNull(minPrice)) {
			criteria = criteria.and("price.regular").gte(minPrice);
		}

		if (Objects.nonNull(maxPrice)) {
			criteria = criteria.and("price.regular").lte(maxPrice);
		}

		if (Objects.nonNull(inStock) && inStock) {
			criteria = criteria.and("stock.available").gt(0);
		}

		Query filterQuery = new Query(criteria);

		return executePagedQuery(filterQuery, pageable, traceId);
	}

	@Override
	public Flux<ProductSummary> findRelatedProducts(UUID productId, int limit, String traceId) {
		log.debug("Finding related products for product: {}, traceId: {}", productId, traceId);

		return mongoTemplate.findById(productId, ProductReadModel.class)
				.flatMapMany(product -> {
					if (product.getCategoryIds().isEmpty()) {
						return Flux.empty();
					}

					Criteria criteria = Criteria.where("categoryIds")
							.in(product.getCategoryIds())
							.and("status").is("ACTIVE")
							.and("id").ne(productId);
					Query relatedQuery = new Query(criteria).limit(limit);

					return mongoTemplate.find(relatedQuery, ProductReadModel.class)
							.flatMap(relatedProduct -> cacheService.getProductSummary(relatedProduct.getId())
									.switchIfEmpty(Mono.defer(() -> {
										ProductSummary summary = ProductMapper.toProductSummary(relatedProduct);
										summary.setTraceId(traceId);
										return cacheService.cacheProductSummary(summary)
												.thenReturn(summary);
									}))
							)
							.doOnNext(dto -> dto.setTraceId(traceId));
				})
				.switchIfEmpty(Flux.empty());
	}

	private Mono<Page<ProductSummary>> executePagedQuery(
			Query query, Pageable pageable, String traceId) {

		Query countQuery = Query.of(query).skip(0).limit(0);

		query.with(pageable);

		return mongoTemplate.count(countQuery, ProductReadModel.class)
				.flatMap(count ->
						mongoTemplate.find(query, ProductReadModel.class)
								.flatMap(product -> {
									// Sprawdź czy podsumowanie jest w cache
									return cacheService.getProductSummary(product.getId())
											.switchIfEmpty(Mono.defer(() -> {
												ProductSummary summary = ProductMapper.toProductSummary(product);
												summary.setTraceId(traceId);
												cacheService.cacheProductSummary(summary).subscribe();
												return Mono.just(summary);
											}));
								})
								.doOnNext(summary -> summary.setTraceId(traceId))
								.collectList()
								.map(list -> new PageImpl<>(list, pageable, count))
				);
	}

	@Override
	public Mono<UpdateResult> updatePrice(UUID productId, BigDecimal price, BigDecimal discountedPrice,
										  String traceId, String spanId) {
		log.debug("Updating price for product: {}, price: {}, discountedPrice: {}, traceId: {}",
				productId, price, discountedPrice, traceId);

		Query query = Query.query(Criteria.where("_id").is(productId));
		Update update = new Update()
				.set("price.regular", price)
				.set("price.discounted", discountedPrice)
				.set("updatedAt", Instant.now())
				.set("lastTraceId", traceId)
				.set("lastSpanId", spanId)
				.set("lastOperation", "UpdatePrice")
				.set("lastUpdatedAt", Instant.now());

		return updateProduct(productId, update, traceId);
	}

	@Override
	public Mono<UpdateResult> updateStock(UUID productId, int quantity, String warehouseId,
										  String traceId, String spanId) {
		log.debug("Updating stock for product: {}, quantity: {}, warehouseId: {}, traceId: {}",
				productId, quantity, warehouseId, traceId);

		Query query = Query.query(Criteria.where("_id").is(productId));
		Update update = new Update()
				.set("stock.available", quantity)
				.set("stock.warehouseId", warehouseId)
				.set("updatedAt", Instant.now())
				.set("lastTraceId", traceId)
				.set("lastSpanId", spanId)
				.set("lastOperation", "UpdateStock")
				.set("lastUpdatedAt", Instant.now());

		return updateProduct(productId, update, traceId);
	}

	@Override
	public Mono<UpdateResult> updateField(UUID id, String field, Object value, String traceId) {
		log.debug("Updating field '{}' for product: {}, traceId: {}", field, id, traceId);

		Query query = Query.query(Criteria.where("_id").is(id));
		Update update = new Update()
				.set(field, value)
				.set("lastTraceId", traceId)
				.set("lastUpdatedAt", Instant.now());

		return updateProduct(id, update, traceId);
	}

	@Override
	public Mono<UpdateResult> updateProduct(UUID id, Update update, String traceId) {
		return mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id)), update, ProductReadModel.class)
				.map(result -> new UpdateResult(
						result.getMatchedCount(),
						result.getModifiedCount(),
						result.wasAcknowledged()
				))
				.doOnSuccess(result -> log.debug("Product update successful: {}, modified: {}, traceId: {}",
						id, result.getModifiedCount(), traceId))
				.doOnError(error -> log.error("Error updating product: {}, traceId: {}",
						error.getMessage(), traceId, error));
	}

	@Override
	public Mono<UpdateResult> markAsDeleted(UUID productId, String traceId, String spanId) {
		log.debug("Marking product as deleted: {}, traceId: {}", productId, traceId);

		Update update = new Update()
				.set("status", "DELETED")
				.set("updatedAt", Instant.now())
				.set("lastTraceId", traceId)
				.set("lastSpanId", spanId)
				.set("lastOperation", "DeleteProduct")
				.set("lastUpdatedAt", Instant.now());

		return updateProduct(productId, update, traceId);
	}
}