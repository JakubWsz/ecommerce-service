package pl.ecommerce.product.read.infrastructure.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.product.read.domain.model.CategoryReadModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CategoryReadRepository extends ReactiveMongoRepository<CategoryReadModel, UUID>, CategoryReadRepositoryCustom {

	Mono<CategoryReadModel> findBySlug(String slug);

	Flux<CategoryReadModel> findByParentCategoryId(UUID parentId);

	Flux<CategoryReadModel> findByParentCategoryIdIsNull();

	@Query("{ 'name': { $regex: ?0, $options: 'i' } }")
	Flux<CategoryReadModel> searchByName(String namePattern);

	Mono<Long> countByParentCategoryId(UUID parentId);

	Mono<Boolean> existsBySlug(String slug);

	Flux<CategoryReadModel> findByActiveTrue();

	Flux<CategoryReadModel> findBySubcategoryIdsContaining(UUID subcategoryId);
}