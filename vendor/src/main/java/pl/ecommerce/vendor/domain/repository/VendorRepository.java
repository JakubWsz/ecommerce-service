package pl.ecommerce.vendor.domain.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.vendor.domain.model.Vendor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface VendorRepository extends ReactiveMongoRepository<Vendor, UUID> {

	Mono<Boolean> existsByEmail(String email);

	Flux<Vendor> findByActiveTrue();
}