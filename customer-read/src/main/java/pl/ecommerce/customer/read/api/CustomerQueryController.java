package pl.ecommerce.customer.read.api;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.customer.model.CustomerStatus;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.customer.read.aplication.dto.CustomerResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerSummary;
import pl.ecommerce.customer.read.aplication.service.CustomerQueryService;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customers", description = "Read operations for customer data")
public class CustomerQueryController implements CustomerApi {

	private final CustomerQueryService customerQueryService;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<CustomerResponse>> getCustomerById(UUID id, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getCustomerById");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get customer with id: {}, traceId: {}", id, traceId);

		return withObservation("getCustomerById", traceId,
				customerQueryService.findById(id, tracingContext));
	}

	@Override
	public Mono<ResponseEntity<CustomerResponse>> getCustomerByEmail(String email, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getCustomerByEmail");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get customer with email: {}, traceId: {}", email, traceId);

		return withObservation("getCustomerByEmail", traceId,
				customerQueryService.findByEmail(email, tracingContext));
	}

	@Override
	public Mono<ResponseEntity<Page<CustomerSummary>>> getCustomerByStatus(String status, ServerWebExchange exchange,
																		   int page, int size, String sortBy, String sortDir) {
		TracingContext tracingContext = createTracingContext(exchange, "getCustomerByStatus");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get customers by status. page={}, size={}, traceId={}", page, size, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("getCustomerByStatus", traceId,
				customerQueryService.findByStatus(CustomerStatus.valueOf(status), tracingContext, pageRequest)
		);
	}

	@Override
	public Mono<ResponseEntity<Page<CustomerSummary>>> getAllCustomers(int page, int size, String sortBy, String sortDir, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getAllCustomers");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get all customers. page={}, size={}, traceId={}", page, size, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("getAllCustomers", traceId,
				customerQueryService.findAllActive(pageRequest, tracingContext)
		);
	}

	@Override
	public Mono<ResponseEntity<Page<CustomerSummary>>> searchCustomers(String query, int page, int size, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "searchCustomers");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to search customers with query: {}, traceId={}", query, traceId);

		PageRequest pageRequest = PageRequest.of(page, size);

		return withObservation("searchCustomers", traceId,
				customerQueryService.searchByName(query, pageRequest, tracingContext)
		);
	}

	private <T> Mono<ResponseEntity<T>> withObservation(String opName, String traceId, Mono<T> mono) {
		return Objects.requireNonNull(Observation.createNotStarted(opName, observationRegistry)
						.observe(() -> mono))
				.map(result -> ResponseEntity.status(HttpStatus.OK)
						.header("X-Trace-Id", traceId)
						.body(result));
	}

	private TracingContext createTracingContext(ServerWebExchange exchange, String operation) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String traceId = headers.getFirst("X-Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}
		String userId = headers.getFirst("X-User-Id");
		return TracingContext.builder()
				.traceId(traceId)
				.spanId(UUID.randomUUID().toString())
				.userId(userId)
				.sourceService("customer-read")
				.sourceOperation(operation)
				.build();
	}
}
