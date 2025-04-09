package pl.ecommerce.customer.read.aplication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;
import pl.ecommerce.customer.read.infrastructure.repository.CustomerReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerQueryService {

	private final CustomerReadRepository customerRepository;

	public Mono<CustomerReadModel> findById(UUID customerId) {
		log.info("Finding customer read model by ID: {}", customerId);
		return customerRepository.findById(customerId)
				.doOnNext(CustomerQueryService::logInfo);
	}

	public Mono<CustomerReadModel> findByEmail(String email) {
		log.info("Finding customer read model by email: {}", email);
		return customerRepository.findByEmail(email)
				.doOnNext(CustomerQueryService::logInfo);
	}

	public Mono<Tuple2<List<CustomerReadModel>, Long>> findByStatus(CustomerStatus customerStatus, Pageable pageable) {
		log.info("Finding customer read models by status: {}", customerStatus);
		Flux<CustomerReadModel> customersFlux = customerRepository.findByStatus(customerStatus, pageable);
		Mono<Long> countMono = customerRepository.countByStatus(customerStatus);

		return Mono.zip(customersFlux.collectList(), countMono);
	}

	public Mono<Tuple2<List<CustomerReadModel>, Long>> findAllActive(Pageable pageable) {
		log.info("Finding all active customer read models");
		return findByStatus(CustomerStatus.ACTIVE, pageable);
	}

	public Mono<Tuple2<List<CustomerReadModel>, Long>> searchByName(String nameQuery, Pageable pageable) {
		log.info("Searching active customer read models by name: {}", nameQuery);
		Flux<CustomerReadModel> customersFlux = customerRepository
				.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(nameQuery, nameQuery)
				.filter(customer -> customer.getStatus() == CustomerStatus.ACTIVE)
				.skip(pageable.getOffset())
				.take(pageable.getPageSize());

		Mono<Long> countMono = customerRepository
				.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(nameQuery, nameQuery)
				.filter(customer -> customer.getStatus() == CustomerStatus.ACTIVE)
				.count();

		return Mono.zip(customersFlux.collectList(), countMono);
	}

	private static void logInfo(CustomerReadModel model) {
		log.debug("Found customer read model: {}", model.getId());
	}
}