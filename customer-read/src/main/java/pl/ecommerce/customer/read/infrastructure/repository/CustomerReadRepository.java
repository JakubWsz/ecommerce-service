package pl.ecommerce.customer.read.infrastructure.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pl.ecommerce.commons.customer.model.CustomerStatus;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CustomerReadRepository extends ReactiveMongoRepository<CustomerReadModel, UUID> {

	Mono<CustomerReadModel> findByEmail(String email);

	Flux<CustomerReadModel> findByStatus(CustomerStatus status);

	Flux<CustomerReadModel> findByStatus(CustomerStatus status, Pageable pageable);

	Mono<Long> countByStatus(CustomerStatus status);

	Mono<Boolean> existsByEmail(String email);

	Flux<CustomerReadModel> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
			String firstName, String lastName);
}