package pl.ecommerce.customer.write.infrastructure.repository;

import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerRepository {

	Mono<CustomerAggregate> save(CustomerAggregate customer);

	Mono<CustomerAggregate> findById(UUID customerId);

	Mono<CustomerAggregate> findByEmail(String email);

	Mono<Boolean> existsByEmail(String email);

	Mono<Void> hardDelete(UUID customerId);
}