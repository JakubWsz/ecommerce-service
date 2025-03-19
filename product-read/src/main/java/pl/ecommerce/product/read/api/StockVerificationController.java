package pl.ecommerce.product.read.api;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.StockVerificationRequest;
import pl.ecommerce.product.read.api.dto.StockVerificationResponse;
import pl.ecommerce.product.read.application.service.StockVerificationService;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/stock")
@Tag(name = "Stock Verification", description = "API for verifying product stock before purchase")
@RequiredArgsConstructor
@Slf4j
public class StockVerificationController implements StockVerificationApi {

	private final StockVerificationService stockVerificationService;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<StockVerificationResponse>> verifyStock(
			@RequestBody StockVerificationRequest request,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "verifyStock");
		String traceId = tracingContext.getTraceId();
		log.debug("Verifying stock for order items: {}, traceId: {}",
				request.getItems().size(), traceId);

		return withObservation("verifyStock", traceId,
				stockVerificationService.verifyStock(request, tracingContext))
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Boolean>> isProductAvailable(
			@PathVariable UUID productId,
			@RequestParam(defaultValue = "1") int quantity,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "isProductAvailable");
		String traceId = tracingContext.getTraceId();
		log.debug("Checking availability of product: {}, quantity: {}, traceId: {}",
				productId, quantity, traceId);

		return withObservation("isProductAvailable", traceId,
				stockVerificationService.isProductAvailable(productId, quantity, tracingContext))
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<StockVerificationResponse>> verifyMultipleProducts(
			@RequestBody StockVerificationRequest request,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "verifyMultipleProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Verifying multiple products, item count: {}, traceId: {}",
				request.getItems().size(), traceId);

		return withObservation("verifyMultipleProducts", traceId,
				stockVerificationService.verifyStock(request, tracingContext))
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Boolean>> reserveStock(
			@PathVariable UUID productId,
			@RequestParam int quantity,
			@RequestParam String reservationId,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "reserveStock");
		String traceId = tracingContext.getTraceId();
		log.debug("Reserving stock for product: {}, quantity: {}, reservationId: {}, traceId: {}",
				productId, quantity, reservationId, traceId);

		return withObservation("reserveStock", traceId,
				stockVerificationService.reserveProductStock(productId, quantity, reservationId, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.badRequest().body(false));
	}

	@Override
	public Mono<ResponseEntity<Boolean>> releaseReservation(
			@PathVariable UUID productId,
			@PathVariable String reservationId,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "releaseReservation");
		String traceId = tracingContext.getTraceId();
		log.debug("Releasing reservation for product: {}, reservationId: {}, traceId: {}",
				productId, reservationId, traceId);

		return withObservation("releaseReservation", traceId,
				stockVerificationService.releaseReservation(productId, reservationId, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.badRequest().body(false));
	}

	private static TracingContext createTracingContext(ServerWebExchange exchange, String operation) {
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
				.sourceService("product-read")
				.sourceOperation(operation)
				.build();
	}

	private <T> Mono<T> withObservation(String operationName, String traceId, Mono<T> mono) {
		return Mono.defer(() -> {
			Observation observation = Observation.createNotStarted(operationName, observationRegistry)
					.contextualName("product-read." + operationName)
					.highCardinalityKeyValue("traceId", traceId);

			return mono.doOnSubscribe(s -> observation.start())
					.doOnTerminate(observation::stop)
					.doOnError(observation::error);
		});
	}
}