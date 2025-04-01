package pl.ecommerce.customer.read.aplication.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.customer.read.aplication.dto.CustomerResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerSummary;
import pl.ecommerce.customer.read.aplication.mapper.CustomerMapper;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;
import pl.ecommerce.customer.read.infrastructure.repository.CustomerReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerQueryService {

	private final CustomerReadRepository customerRepository;
	private final ObservationRegistry observationRegistry;

	public Mono<CustomerResponse> findById(UUID customerId, TracingContext tracingContext) {
		String traceId = getTraceId(tracingContext);
		log.debug("Finding customer by ID: {}, traceId: {}", customerId, traceId);

		return observe("customer.findById",
				customerRepository.findById(customerId)
						.doOnNext(model -> log.debug("Found customer: {}, traceId: {}", model.getId(), traceId))
						.map(CustomerMapper::toCustomerResponse)
						.doOnNext(dto -> dto.setTraceId(traceId)));
	}

	public Mono<CustomerResponse> findByEmail(String email, TracingContext tracingContext) {
		String traceId = getTraceId(tracingContext);
		log.debug("Finding customer by email: {}, traceId: {}", email, traceId);

		return observe("customer.findByEmail",
				customerRepository.findByEmail(email)
						.doOnNext(model -> log.debug("Found customer: {}, traceId: {}", model.getId(), traceId))
						.map(CustomerMapper::toCustomerResponse)
						.doOnNext(dto -> dto.setTraceId(traceId)));
	}

	public Mono<Page<CustomerSummary>> findByStatus(CustomerStatus customerStatus, TracingContext tracingContext, Pageable pageable) {
		String traceId = getTraceId(tracingContext);
		log.debug("Finding customers by status: {}, traceId: {}", customerStatus, traceId);

		Flux<CustomerReadModel> customersFlux = customerRepository.findByStatus(customerStatus, pageable);
		Mono<Long> countMono = customerRepository.countByStatus(customerStatus);

		return observe("customer.findByStatus", buildPage(pageable, customersFlux, countMono, traceId));
	}

	public Mono<Page<CustomerSummary>> findAllActive(Pageable pageable, TracingContext tracingContext) {
		String traceId = getTraceId(tracingContext);
		log.debug("Finding all active customers with pagination, traceId: {}", traceId);

		Flux<CustomerReadModel> customersFlux = customerRepository.findByStatus(CustomerStatus.ACTIVE, pageable);
		Mono<Long> countMono = customerRepository.countByStatus(CustomerStatus.ACTIVE);

		return observe("customer.findAllActive", buildPage(pageable, customersFlux, countMono, traceId));
	}

	public Mono<Page<CustomerSummary>> searchByName(String nameQuery, Pageable pageable, TracingContext tracingContext) {
		String traceId = getTraceId(tracingContext);
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

		return observe("customer.searchByName", buildPage(pageable, customersFlux, countMono, traceId));
	}

	private String getTraceId(TracingContext tracingContext) {
		return nonNull(tracingContext) ? tracingContext.getTraceId() : "unknown";
	}

	private <T> Mono<T> observe(String opName, Mono<T> mono) {
		return Observation.createNotStarted(opName, observationRegistry)
				.observe(() -> mono);
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