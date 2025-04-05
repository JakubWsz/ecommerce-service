package pl.ecommerce.customer.read.api;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.commons.tracing.TracedOperation;
import pl.ecommerce.commons.tracing.TracingAspect;
import pl.ecommerce.customer.read.aplication.dto.CustomerResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerSummary;
import pl.ecommerce.customer.read.aplication.service.CustomerQueryService;
import pl.ecommerce.commons.tracing.TraceService;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerQueryController implements CustomerApi {

	private final CustomerQueryService customerQueryService;
	private final TraceService traceService;

	@Override
	@TracedOperation("getCustomerById")
	public Mono<ResponseEntity<CustomerResponse>> getCustomerById(UUID id, ServerWebExchange exchange) {
		return executeWithSpan("get-customer-by-id", traceId -> {
			Span.current().setAttribute("customer.id", id.toString());
			log.info("Received request to get customer with id: {}, traceId: {}", id, traceId);
			return customerQueryService.findById(id)
					.map(customer -> ResponseEntity.ok()
							.header("X-Trace-Id", traceId)
							.body(customer));
		});
	}

	@Override
	@TracedOperation("getCustomerByEmail")
	public Mono<ResponseEntity<CustomerResponse>> getCustomerByEmail(String email, ServerWebExchange exchange) {
		return executeWithSpan("get-customer-by-email", traceId -> {
			Span.current().setAttribute("request.email", email);
			log.info("Received request to get customer with email: {}, traceId: {}", email, traceId);
			return customerQueryService.findByEmail(email)
					.map(customer -> ResponseEntity.ok()
							.header("X-Trace-Id", traceId)
							.body(customer));
		});
	}

	@Override
	@TracedOperation("getCustomerByStatus")
	public Mono<ResponseEntity<Page<CustomerSummary>>> getCustomerByStatus(String status, ServerWebExchange exchange,
																		   int page, int size, String sortBy, String sortDir) {
		return executeWithSpan("get-customer-by-status", traceId -> {
			Span.current().setAttribute("customer.status", status);
			Span.current().setAttribute("page", String.valueOf(page));
			Span.current().setAttribute("size", String.valueOf(size));
			log.info("Received request to get customers by status. page={}, size={}, traceId={}", page, size, traceId);
			Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
			PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
			return customerQueryService.findByStatus(CustomerStatus.valueOf(status), pageRequest)
					.map(result -> ResponseEntity.ok()
							.header("X-Trace-Id", traceId)
							.body(result));
		});
	}

	@Override
	@TracedOperation("getAllCustomers")
	public Mono<ResponseEntity<Page<CustomerSummary>>> getAllCustomers(int page, int size, String sortBy, String sortDir, ServerWebExchange exchange) {
		return executeWithSpan("get-all-customers", traceId -> {
			Span.current().setAttribute("page", String.valueOf(page));
			Span.current().setAttribute("size", String.valueOf(size));
			log.info("Received request to get all customers. page={}, size={}, traceId={}", page, size, traceId);
			Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
			PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
			return customerQueryService.findAllActive(pageRequest)
					.map(result -> ResponseEntity.ok()
							.header("X-Trace-Id", traceId)
							.body(result));
		});
	}

	@Override
	@TracedOperation("searchCustomers")
	public Mono<ResponseEntity<Page<CustomerSummary>>> searchCustomers(String query, int page, int size, ServerWebExchange exchange) {
		return executeWithSpan("search-customers", traceId -> {
			Span.current().setAttribute("search.query", query);
			log.info("Received request to search customers with query: {}, traceId: {}", query, traceId);
			PageRequest pageRequest = PageRequest.of(page, size);
			return customerQueryService.searchByName(query, pageRequest)
					.map(result -> ResponseEntity.ok()
							.header("X-Trace-Id", traceId)
							.body(result));
		});
	}

	private <T> Mono<T> executeWithSpan(String operation, Function<String, Mono<T>> action) {
		return Mono.deferContextual(ctx -> {
			String traceId = ctx.getOrDefault(TracingAspect.TRACE_ID_CONTEXT_KEY, traceService.getCurrentTraceId());
			Span span = GlobalOpenTelemetry.getTracer("customer-read")
					.spanBuilder(operation)
					.setSpanKind(SpanKind.INTERNAL)
					.startSpan();
			try (io.opentelemetry.context.Scope scope = span.makeCurrent()) {
				return action.apply(traceId)
						.doOnTerminate(span::end)
						.doOnError(span::recordException);
			}
		});
	}
}
