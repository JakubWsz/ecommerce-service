package pl.ecommerce.vendor.write.infrastructure.repository;

import pl.ecommerce.vendor.write.domain.VendorAggregate;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface VendorRepository {
	Mono<VendorAggregate> findById(UUID id);
	Mono<VendorAggregate> save(VendorAggregate vendor);
	Mono<Boolean> existsByEmail(String email);
}