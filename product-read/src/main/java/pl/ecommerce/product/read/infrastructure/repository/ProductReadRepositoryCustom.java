package pl.ecommerce.product.read.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public interface ProductReadRepositoryCustom {

	Mono<Page<ProductSummary>> searchProducts(String query, Pageable pageable, String traceId);

	Mono<Page<ProductSummary>> findByCategory(UUID categoryId, Pageable pageable, String traceId);

	Mono<Page<ProductSummary>> findByVendor(UUID vendorId, Pageable pageable, String traceId);

	Mono<Page<ProductSummary>> findFeaturedProducts(Pageable pageable, String traceId);

	Mono<Page<ProductSummary>> filterProducts(
			Set<UUID> categories,
			Set<UUID> vendors,
			UUID brandId,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean inStock,
			Pageable pageable,
			String traceId);

	Flux<ProductSummary> findRelatedProducts(UUID productId, int limit, String traceId);

	Mono<UpdateResult> updatePrice(UUID productId, BigDecimal price, BigDecimal discountedPrice, String currency,
								   String traceId, String spanId);

	Mono<UpdateResult> updateStock(UUID productId, int quantity, String warehouseId, String traceId, String spanId);

	Mono<UpdateResult> updateField(UUID id, String field, Object value, String traceId);

	Mono<UpdateResult> updateProduct(UUID id, Update update, String traceId);

	Mono<UpdateResult> markAsDeleted(UUID productId, String traceId, String spanId);
}