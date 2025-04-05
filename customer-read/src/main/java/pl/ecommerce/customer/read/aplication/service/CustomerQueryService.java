package pl.ecommerce.customer.read.aplication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.customer.read.aplication.dto.CustomerResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerSummary;
import pl.ecommerce.customer.read.aplication.mapper.CustomerMapper;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;
import pl.ecommerce.customer.read.infrastructure.repository.CustomerReadRepository;
import pl.ecommerce.commons.tracing.TraceService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerQueryService {

	private final CustomerReadRepository customerRepository;
	private final TraceService traceService;

	public Mono<CustomerResponse> findById(UUID customerId) {
		String traceId = traceService.getCurrentTraceId();
		log.debug("Finding customer by ID: {}, traceId: {}", customerId, traceId);
		return customerRepository.findById(customerId)
				.doOnNext(model ->
						log.debug("Found customer: {}, traceId: {}", model.getId(), traceId))
				.map(CustomerMapper::toCustomerResponse)
				.doOnNext(dto -> dto.setTraceId(traceId));
	}

	public Mono<CustomerResponse> findByEmail(String email) {
		String traceId = traceService.getCurrentTraceId();
		log.debug("Finding customer by email: {}, traceId: {}", email, traceId);
		return customerRepository.findByEmail(email)
				.doOnNext(model ->
						log.debug("Found customer: {}, traceId: {}", model.getId(), traceId))
				.map(CustomerMapper::toCustomerResponse)
				.doOnNext(dto -> dto.setTraceId(traceId));
	}

	public Mono<Page<CustomerSummary>> findByStatus(CustomerStatus customerStatus, Pageable pageable) {
		String traceId = traceService.getCurrentTraceId();
		log.debug("Finding customers by status: {}, traceId: {}", customerStatus, traceId);
		Flux<CustomerReadModel> customersFlux = customerRepository.findByStatus(customerStatus, pageable);
		Mono<Long> countMono = customerRepository.countByStatus(customerStatus);
		return buildPage(pageable, customersFlux, countMono, traceId);
	}

	public Mono<Page<CustomerSummary>> findAllActive(Pageable pageable) {
		String traceId = traceService.getCurrentTraceId();
		log.debug("Finding all active customers with pagination, traceId: {}", traceId);
		Flux<CustomerReadModel> customersFlux = customerRepository.findByStatus(CustomerStatus.ACTIVE, pageable);
		Mono<Long> countMono = customerRepository.countByStatus(CustomerStatus.ACTIVE);
		return buildPage(pageable, customersFlux, countMono, traceId);
	}

	public Mono<Page<CustomerSummary>> searchByName(String nameQuery, Pageable pageable) {
		String traceId = traceService.getCurrentTraceId();
		log.debug("Searching customers by name: {}, traceId: {}", nameQuery, traceId);
		Flux<CustomerReadModel> customersFlux = customerRepository
				.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(nameQuery, nameQuery)
				.filter(customer -> customer.getStatus() == CustomerStatus.ACTIVE)
				.skip(pageable.getOffset())
				.take(pageable.getPageSize());
		Mono<Long> countMono = customerRepository
				.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(nameQuery, nameQuery)
				.filter(customer -> customer.getStatus() == CustomerStatus.ACTIVE)
				.count();
		return buildPage(pageable, customersFlux, countMono, traceId);
	}

	private Mono<Page<CustomerSummary>> buildPage(Pageable pageable, Flux<CustomerReadModel> flux, Mono<Long> countMono, String traceId) {
		return flux
				.map(CustomerMapper::toCustomerSummary)
				.doOnNext(dto -> dto.setTraceId(traceId))
				.collectList()
				.zipWith(countMono)
				.map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
	}
}
