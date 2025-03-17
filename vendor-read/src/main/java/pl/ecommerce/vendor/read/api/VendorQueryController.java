package pl.ecommerce.vendor.read.api;

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
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.vendor.read.api.dto.VendorResponse;
import pl.ecommerce.vendor.read.api.dto.VendorSummary;
import pl.ecommerce.vendor.read.application.VendorQueryService;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vendors", description = "Read operations for vendor data")
public class VendorQueryController implements VendorQueryApi {

	private final VendorQueryService vendorQueryService;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<VendorResponse>> getVendorById(UUID id, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getVendorById");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get vendor with id: {}, traceId: {}", id, traceId);

		return withObservation("getVendorById", traceId,
				vendorQueryService.findById(id, tracingContext));
	}

	@Override
	public Mono<ResponseEntity<VendorResponse>> getVendorByEmail(String email, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getVendorByEmail");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get vendor with email: {}, traceId: {}", email, traceId);

		return withObservation("getVendorByEmail", traceId,
				vendorQueryService.findByEmail(email, tracingContext));
	}

	@Override
	public Mono<ResponseEntity<Page<VendorSummary>>> getVendorsByStatus(String status, ServerWebExchange exchange,
																		int page, int size, String sortBy, String sortDir) {
		TracingContext tracingContext = createTracingContext(exchange, "getVendorsByStatus");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get vendors by status: {}. page={}, size={}, traceId={}",
				status, page, size, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("getVendorsByStatus", traceId,
				vendorQueryService.findByStatus(VendorStatus.valueOf(status), tracingContext, pageRequest)
		);
	}

	@Override
	public Mono<ResponseEntity<Page<VendorSummary>>> getAllVendors(int page, int size,
																   String sortBy, String sortDir,
																   ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getAllVendors");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get all vendors. page={}, size={}, traceId={}", page, size, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("getAllVendors", traceId,
				vendorQueryService.findAllActive(pageRequest, tracingContext)
		);
	}

	@Override
	public Mono<ResponseEntity<Page<VendorSummary>>> searchVendors(String query, int page, int size,
																   ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "searchVendors");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to search vendors with query: {}, traceId={}", query, traceId);

		PageRequest pageRequest = PageRequest.of(page, size);

		return withObservation("searchVendors", traceId,
				vendorQueryService.searchByName(query, pageRequest, tracingContext)
		);
	}

	@Override
	public Mono<ResponseEntity<Page<VendorSummary>>> getVendorsByCategory(UUID categoryId, int page, int size,
																		  ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getVendorsByCategory");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to get vendors by category id: {}, traceId={}", categoryId, traceId);

		PageRequest pageRequest = PageRequest.of(page, size);

		return withObservation("getVendorsByCategory", traceId,
				vendorQueryService.findByCategory(categoryId, pageRequest, tracingContext)
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
				.sourceService("vendor-read")
				.sourceOperation(operation)
				.build();
	}
}
