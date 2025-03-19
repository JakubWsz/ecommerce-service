package pl.ecommerce.product.read.api;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.ProductResponse;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.application.service.ProductQueryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductQueryController implements ProductApi {

	private final ProductQueryService productService;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<ProductResponse>> getProductById(UUID id, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getProductById");
		String traceId = tracingContext.getTraceId();
		log.debug("Getting product by ID: {}, traceId: {}", id, traceId);

		return withObservation("getProductById", traceId,
				productService.findById(id, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<ProductResponse>> getProductBySku(String sku, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getProductBySku");
		String traceId = tracingContext.getTraceId();
		log.debug("Getting product by SKU: {}, traceId: {}", sku, traceId);

		return withObservation("getProductBySku", traceId,
				productService.findBySku(sku, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<org.springframework.data.domain.Page<ProductSummary>>> searchProducts(
			String query, int page, int size, String sortBy, String sortDir, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "searchProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Searching products with query: {}, page: {}, size: {}, traceId: {}",
				query, page, size, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
				Sort.Direction.DESC : Sort.Direction.ASC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("searchProducts", traceId,
				productService.searchProducts(query, pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<org.springframework.data.domain.Page<ProductSummary>>> getProductsByCategory(
			UUID categoryId, int page, int size, String sortBy, String sortDir, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "getProductsByCategory");
		String traceId = tracingContext.getTraceId();
		log.debug("Getting products by category: {}, page: {}, size: {}, traceId: {}",
				categoryId, page, size, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
				Sort.Direction.DESC : Sort.Direction.ASC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("getProductsByCategory", traceId,
				productService.findByCategory(categoryId, pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<org.springframework.data.domain.Page<ProductSummary>>> getProductsByVendor(
			UUID vendorId, int page, int size, String sortBy, String sortDir, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "getProductsByVendor");
		String traceId = tracingContext.getTraceId();
		log.debug("Getting products by vendor: {}, page: {}, size: {}, traceId: {}",
				vendorId, page, size, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
				Sort.Direction.DESC : Sort.Direction.ASC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("getProductsByVendor", traceId,
				productService.findByVendor(vendorId, pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<org.springframework.data.domain.Page<ProductSummary>>> getFeaturedProducts(
			int page, int size, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "getFeaturedProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Getting featured products, page: {}, size: {}, traceId: {}",
				page, size, traceId);

		Pageable pageable = PageRequest.of(page, size);

		return withObservation("getFeaturedProducts", traceId,
				productService.findFeaturedProducts(pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<org.springframework.data.domain.Page<ProductSummary>>> filterProducts(
			Set<UUID> categories, Set<UUID> vendors, UUID brandId, BigDecimal minPrice,
			BigDecimal maxPrice, Boolean inStock, int page, int size, String sortBy,
			String sortDir, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "filterProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Filtering products, categories: {}, vendors: {}, brandId: {}, price range: {}-{}, inStock: {}, traceId: {}",
				categories, vendors, brandId, minPrice, maxPrice, inStock, traceId);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
				Sort.Direction.DESC : Sort.Direction.ASC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return withObservation("filterProducts", traceId,
				productService.filterProducts(categories, vendors, brandId, minPrice, maxPrice,
						inStock, pageable, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<Flux<ProductSummary>>> getRelatedProducts(
			UUID id, int limit, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "getRelatedProducts");
		String traceId = tracingContext.getTraceId();
		log.debug("Getting related products for product: {}, limit: {}, traceId: {}",
				id, limit, traceId);

		return withObservation("getRelatedProducts", traceId,
				productService.findRelatedProducts(id, limit, tracingContext))
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
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