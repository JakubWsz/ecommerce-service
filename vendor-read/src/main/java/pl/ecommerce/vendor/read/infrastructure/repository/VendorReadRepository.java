package pl.ecommerce.vendor.read.infrastructure.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.vendor.read.domain.VendorReadModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface VendorReadRepository extends ReactiveMongoRepository<VendorReadModel, UUID> {

	Mono<VendorReadModel> findByEmail(String email);

	Flux<VendorReadModel> findByStatus(VendorStatus status, Pageable pageable);

	Mono<Long> countByStatus(VendorStatus status);

	Flux<VendorReadModel> findByNameContainingIgnoreCaseOrBusinessNameContainingIgnoreCase(
			String name, String businessName, Pageable pageable);

	Mono<Long> countByNameContainingIgnoreCaseOrBusinessNameContainingIgnoreCase(
			String name, String businessName);

	Flux<VendorReadModel> findByCategoriesCategoryId(UUID categoryId, Pageable pageable);

	Mono<Long> countByCategoriesCategoryId(UUID categoryId);

	Mono<Boolean> existsByEmail(String email);
}