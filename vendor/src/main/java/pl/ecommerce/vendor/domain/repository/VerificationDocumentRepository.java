package pl.ecommerce.vendor.domain.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface VerificationDocumentRepository extends ReactiveMongoRepository<VerificationDocument, UUID> {

	Flux<VerificationDocument> findByVendorId(UUID vendorId);

	Flux<VerificationDocument> findByVendorIdAndStatus(UUID vendorId, String status);

	Flux<VerificationDocument> findByVendorIdAndDocumentType(UUID vendorId, String documentType);

	Mono<Long> countByVendorIdAndStatus(UUID vendorId, String status);

	Flux<VerificationDocument> findByStatus(String status);

	Mono<VerificationDocument> findFirstByVendorIdOrderByCreatedAtDesc(UUID vendorId);
}

