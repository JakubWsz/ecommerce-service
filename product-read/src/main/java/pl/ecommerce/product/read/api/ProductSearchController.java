package pl.ecommerce.product.read.api;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.application.service.ProductSearchService;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

/**
 * Kontroler obsługujący zaawansowane wyszukiwanie produktów
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductSearchController implements ProductSearchApi {

	private final ProductSearchService searchService;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<Page<ProductSummary>>> advancedSearch(
			String query,
			List<UUID> categories,
			List<UUID> vendors,
			List<String> brands,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean onlyDiscounted,
			Boolean inStock,
			String attributeName,
			String attributeValue,
			Boolean featured,
			int page,
			int size,
			String sortBy,
			String sortDir,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "advancedSearch");
		String traceId = tracingContext.getTraceId();
		log.debug("Advanced product search request, traceId: {}", traceId);

		Pageable pageable = createPageable(page, size, sortBy, sortDir);

		return withObservation("advancedSearch", traceId,
				searchService.advancedSearch(
						query, categories, vendors, brands, minPrice, maxPrice,
						onlyDiscounted, inStock, attributeName, attributeValue, featured,
						pageable, tracingContext)
		)
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<List<ProductSummary>>> findSimilarProducts(
			UUID productId,
			int limit,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "findSimilarProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Find similar products to: {}, traceId: {}", productId, traceId);

		return withObservation("findSimilarProducts", traceId,
				searchService.findSimilarProducts(productId, limit, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<Page<ProductSummary>>> findByPriceRange(
			BigDecimal minPrice,
			BigDecimal maxPrice,
			int page,
			int size,
			String sortBy,
			String sortDir,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "findByPriceRange");
		String traceId = tracingContext.getTraceId();
		log.debug("Find products in price range: {} - {}, traceId: {}",
				minPrice, maxPrice, traceId);

		Pageable pageable = createPageable(page, size, sortBy, sortDir);

		return withObservation("findByPriceRange", traceId,
				searchService.findProductsByPriceRange(minPrice, maxPrice, pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<Page<ProductSummary>>> findProductsOnSale(
			Integer minDiscountPercentage,
			int page,
			int size,
			String sortBy,
			String sortDir,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "findProductsOnSale");
		String traceId = tracingContext.getTraceId();
		log.debug("Find products on sale, min discount: {}%, traceId: {}",
				minDiscountPercentage, traceId);

		Pageable pageable = createPageable(page, size, sortBy, sortDir);

		return withObservation("findProductsOnSale", traceId,
				searchService.findProductsOnSale(minDiscountPercentage, pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<Page<ProductSummary>>> findProductsByAttributes(
			Map<String, String> attributes,
			int page,
			int size,
			String sortBy,
			String sortDir,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "findProductsByAttributes");
		String traceId = tracingContext.getTraceId();
		log.debug("Find products by attributes: {}, traceId: {}",
				attributes, traceId);

		Map<String, String> attributeFilters = filterAttributeParams(attributes);

		if (attributeFilters.isEmpty()) {
			return Mono.just(ResponseEntity.badRequest().body(Page.empty()));
		}

		Pageable pageable = createPageable(page, size, sortBy, sortDir);

		return withObservation("findProductsByAttributes", traceId,
				searchService.findProductsByAttributes(attributeFilters, pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<List<ProductSummary>>> findLatestProducts(
			int limit,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "findLatestProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Find latest {} products, traceId: {}", limit, traceId);

		return withObservation("findLatestProducts", traceId,
				searchService.findLatestProducts(limit, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<List<ProductSummary>>> findPopularProducts(
			int limit,
			ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "findPopularProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Find popular products, limit: {}, traceId: {}", limit, traceId);

		return withObservation("findPopularProducts", traceId,
				searchService.findPopularProducts(limit, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		return PageRequest.of(page, size, Sort.by(direction, sortBy));
	}

	private Map<String, String> filterAttributeParams(Map<String, String> attributes) {
		Map<String, String> attributeFilters = new HashMap<>(attributes);
		attributeFilters.remove("page");
		attributeFilters.remove("size");
		attributeFilters.remove("sortBy");
		attributeFilters.remove("sortDir");
		return attributeFilters;
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