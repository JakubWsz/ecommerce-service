package pl.ecommerce.customer.infrastructure.repository;

import pl.ecommerce.customer.domain.aggregate.CustomerAggregate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Interfejs repozytorium dla agregatów klienta
 */
public interface CustomerRepository {

	Mono<CustomerAggregate> save(CustomerAggregate customer);


	Mono<CustomerAggregate> findById(UUID customerId);


	Mono<CustomerAggregate> findByEmail(String email);


	Mono<Boolean> existsByEmail(String email);


	Mono<Void> hardDelete(UUID customerId);
	Flux<CustomerAggregate> findAll();
}