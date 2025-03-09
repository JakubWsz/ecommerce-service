package pl.ecommerce.vendor.domain.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CategoryAssignmentRepository extends ReactiveMongoRepository<CategoryAssignment, UUID> {

	Flux<CategoryAssignment> findByVendorId(UUID vendorId);

	Flux<CategoryAssignment> findByVendorIdAndStatus(UUID vendorId, CategoryAssignment.CategoryAssignmentStatus status);

	Mono<CategoryAssignment> findByVendorIdAndCategoryId(UUID vendorId, UUID categoryId);

	Mono<Boolean> existsByVendorIdAndCategoryId(UUID vendorId, UUID categoryId);

}