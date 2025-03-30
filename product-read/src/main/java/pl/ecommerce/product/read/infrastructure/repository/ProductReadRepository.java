package pl.ecommerce.product.read.infrastructure.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ProductReadRepository extends ReactiveMongoRepository<ProductReadModel, UUID>, ProductReadRepositoryCustom{

	Mono<ProductReadModel> findBySku(String sku);

	Flux<ProductReadModel> findByVendorId(UUID vendorId, Pageable pageable);

	Flux<ProductReadModel> findByCategoryIdsContaining(UUID categoryId, Pageable pageable);

	Flux<ProductReadModel> findByFeaturedTrue(Pageable pageable);

	Flux<ProductReadModel> findByNameContainingIgnoreCase(String name, Pageable pageable);

	@Query("{ 'name': { $regex: ?0, $options: 'i' } }")
	Flux<ProductReadModel> searchByName(String namePattern, Pageable pageable);

	@Query("{ $or: [ { 'name': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
	Flux<ProductReadModel> searchByNameOrDescription(String searchPattern, Pageable pageable);

	Mono<Boolean> existsBySku(String sku);

	Mono<Long> countByStatus(String status);

	Mono<Long> countByCategoryIdsContaining(UUID categoryId);

	Mono<Long> countByVendorId(UUID vendorId);

	Mono<UpdateResult> updateStock(UUID productId, int quantity, String warehouseId, String traceId, String spanId);
}