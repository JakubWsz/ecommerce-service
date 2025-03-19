package pl.ecommerce.product.read.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.CategoryResponse;
import pl.ecommerce.product.read.api.dto.CategorySummary;
import pl.ecommerce.product.read.application.service.CategoryQueryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CategoryQueryController implements CategoryApi {

	private final CategoryQueryService categoryService;
	private final TracingContextBuilder tracingContextBuilder;

	@Override
	public Mono<ResponseEntity<CategoryResponse>> getCategoryById(UUID id, ServerWebExchange exchange) {
		TracingContext tracingContext = createTracingContext(exchange, "getCategoryById");
		String traceId = tracingContext.getTraceId();

		return categoryService.findById(id, tracingContext)
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<CategoryResponse>> getCategoryBySlug(String slug, ServerWebExchange exchange) {
		TracingContext tracingContext = tracingContextBuilder.build(exchange);

		return categoryService.findBySlug(slug, tracingContext)
				.map(ResponseEntity::ok)
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@Override
	public Mono<ResponseEntity<Flux<CategorySummary>>> getRootCategories(ServerWebExchange exchange) {
		TracingContext tracingContext = tracingContextBuilder.build(exchange);

		return categoryService.getRootCategories(tracingContext)
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Flux<CategorySummary>>> getSubcategories(UUID id, ServerWebExchange exchange) {
		TracingContext tracingContext = tracingContextBuilder.build(exchange);

		return categoryService.getSubcategories(id, tracingContext)
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Flux<CategoryResponse>>> getCategoryTree(ServerWebExchange exchange) {
		TracingContext tracingContext = tracingContextBuilder.build(exchange);

		return categoryService.getCategoryTree(tracingContext)
				.map(ResponseEntity::ok);
	}

	@Override
	public Mono<ResponseEntity<Flux<CategorySummary>>> searchCategories(String query, ServerWebExchange exchange) {
		TracingContext tracingContext = tracingContextBuilder.build(exchange);

		return categoryService.searchCategories(query, tracingContext)
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
}
