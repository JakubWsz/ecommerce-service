package pl.ecommerce.customer.aplication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import pl.ecommerce.customer.aplication.dto.CustomerReadModel;
import pl.ecommerce.customer.aplication.dto.CustomerSummaryDto;
import pl.ecommerce.customer.domain.exceptions.CustomerNotFoundException;
import pl.ecommerce.customer.domain.valueobjects.CustomerStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Serwis do operacji odczytu na danych klientów.
 * Używa MongoDB jako bazy odczytu zgodnie z wzorcem CQRS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerQueryService {

	private final ReactiveMongoTemplate mongoTemplate;

	/**
	 * Pobiera szczegóły klienta po ID
	 * @param customerId ID klienta
	 * @return Mono z modelem odczytu klienta
	 */
	public Mono<CustomerReadModel> findById(UUID customerId) {
		log.debug("Finding customer by ID: {}", customerId);

		Query query = new Query(Criteria.where("_id").is(customerId.toString()));
		return mongoTemplate.findOne(query, CustomerReadModel.class, "customers")
				.switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)));
	}

	/**
	 * Pobiera klienta po adresie email
	 * @param email Adres email
	 * @return Mono z modelem odczytu klienta
	 */
	public Mono<CustomerReadModel> findByEmail(String email) {
		log.debug("Finding customer by email: {}", email);

		Query query = new Query(Criteria.where("personalData.email").is(email));
		return mongoTemplate.findOne(query, CustomerReadModel.class, "customers");
	}

	/**
	 * Pobiera wszystkich aktywnych klientów
	 * @param pageable Parametry paginacji
	 * @return Page z listą klientów
	 */
	public Mono<Page<CustomerSummaryDto>> findAllActive(Pageable pageable) {
		log.debug("Finding all active customers");

		Query query = new Query(Criteria.where("status").is(CustomerStatus.ACTIVE.name()));
		query.with(pageable);

		Flux<CustomerReadModel> customers = mongoTemplate.find(query, CustomerReadModel.class, "customers");

		Mono<Long> count = mongoTemplate.count(
				Query.of(query).limit(0).skip(0),
				CustomerReadModel.class,
				"customers"
		);

		return Flux.from(customers)
				.map(this::toSummaryDto)
				.collectList()
				.zipWith(count)
				.map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
	}

	/**
	 * Sprawdza czy istnieje klient o podanym adresie email
	 * @param email Adres email
	 * @return Mono z wartością boolean
	 */
	public Mono<Boolean> existsByEmail(String email) {
		Query query = new Query(Criteria.where("personalData.email").is(email));
		return mongoTemplate.exists(query, CustomerReadModel.class, "customers");
	}

	/**
	 * Mapuje pełny model odczytu klienta na uproszczone DTO
	 * @param customerReadModel Model odczytu klienta
	 * @return DTO z podsumowaniem klienta
	 */
	private CustomerSummaryDto toSummaryDto(CustomerReadModel customerReadModel) {
		return CustomerSummaryDto.builder()
				.id(customerReadModel.getId())
				.firstName(customerReadModel.getPersonalData().getFirstName())
				.lastName(customerReadModel.getPersonalData().getLastName())
				.email(customerReadModel.getPersonalData().getEmail())
				.status(customerReadModel.getStatus())
				.createdAt(customerReadModel.getCreatedAt())
				.addressCount(customerReadModel.getAddresses() != null ? customerReadModel.getAddresses().size() : 0)
				.build();
	}
}