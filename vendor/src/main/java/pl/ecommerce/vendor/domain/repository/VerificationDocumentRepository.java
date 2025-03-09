package pl.ecommerce.vendor.domain.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface VerificationDocumentRepository extends ReactiveMongoRepository<VerificationDocument, UUID> {

	Flux<VerificationDocument> findByVendorId(UUID vendorId);
}

