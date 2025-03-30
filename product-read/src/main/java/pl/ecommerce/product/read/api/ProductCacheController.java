package pl.ecommerce.product.read.api;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.CacheStatisticsResponse;
import pl.ecommerce.product.read.application.service.ProductCacheService;
import pl.ecommerce.product.read.infrastructure.cahce.CacheMetricsCollector;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/cache")
@Tag(name = "Cache Management", description = "Administrative endpoints for product cache management")
@RequiredArgsConstructor
@Slf4j
public class ProductCacheController implements ProductCacheApi {

	private final ProductCacheService cacheService;
	private final CacheMetricsCollector metricsCollector;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<Boolean>> refreshProduct(@PathVariable UUID productId, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "refreshProduct");
		String traceId = tracingContext.getTraceId();
		log.debug("Refreshing product cache for ID: {}, traceId: {}", productId, traceId);

		return withObservation("refreshProduct", traceId,
				cacheService.refreshProduct(productId))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<Boolean>> invalidateProduct(@PathVariable UUID productId, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "invalidateProduct");
		String traceId = tracingContext.getTraceId();
		log.debug("Invalidating product cache for ID: {}, traceId: {}", productId, traceId);

		return withObservation("invalidateProduct", traceId,
				cacheService.removeProduct(productId))
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Boolean>> invalidateProductBySku(@PathVariable String sku, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "invalidateProductBySku");
		String traceId = tracingContext.getTraceId();
		log.debug("Invalidating product cache for SKU: {}, traceId: {}", sku, traceId);

		return withObservation("invalidateProductBySku", traceId,
				cacheService.removeProductBySku(sku))
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Boolean>> refreshFeaturedProducts(ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "refreshFeaturedProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Refreshing featured products cache, traceId: {}", traceId);

		return withObservation("refreshFeaturedProducts", traceId,
				cacheService.refreshFeaturedProducts())
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Boolean>> clearAllProductCache(ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "clearAllProductCache");
		String traceId = tracingContext.getTraceId();
		log.debug("Clearing all product cache, traceId: {}", traceId);

		return withObservation("clearAllProductCache", traceId,
				cacheService.clearAllCache())
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<CacheStatisticsResponse>> getCacheStatistics(ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getCacheStatistics");
		String traceId = tracingContext.getTraceId();
		log.debug("Getting cache statistics, traceId: {}", traceId);

		return withObservation("getCacheStatistics", traceId,
				cacheService.getCacheStatistics()
						.map(stats -> {
							CacheStatisticsResponse response = new CacheStatisticsResponse(
									stats.getHits(),
									stats.getMisses(),
									stats.getSize(),
									stats.getHitRatio(),
									stats.getMemoryUsage(),
									traceId
							);

							metricsCollector.recordCacheStats(stats.getHits(), stats.getMisses(), stats.getSize());

							return response;
						}))
				.map(ResponseEntity::ok);
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
					.highCardinalityKeyValue("traceId", traceId)
					.start();

			return mono.doOnTerminate(observation::stop)
					.doOnError(observation::error);
		});
	}
}