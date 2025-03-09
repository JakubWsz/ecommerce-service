package pl.ecommerce.vendor.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${product.service.url}")
	private String productServiceUrl;

	@CircuitBreaker(name = "productService", fallbackMethod = "getCategoryDetailsFallback")
	public Mono<CategoryResponse> getCategoryDetails(UUID categoryId) {
		log.debug("Fetching category details for ID: {}", categoryId);

		return webClientBuilder.baseUrl(productServiceUrl)
				.build()
				.get()
				.uri("/api/v1/categories/{id}", categoryId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(CategoryResponse.class)
				.doOnError(e -> log.error("Error fetching category details: {}", e.getMessage(), e));
	}

	public Mono<CategoryResponse> getCategoryDetailsFallback(String categoryId, Exception e) {
		log.warn("Using fallback for category details. Error: {}", e.getMessage());

		return Mono.just(new CategoryResponse(UUID.fromString(categoryId), "Unknown Category", "Category information unavailable"));
	}

	@CircuitBreaker(name = "productService", fallbackMethod = "categoryExistsFallback")
	public Mono<Boolean> categoryExists(UUID categoryId) {
		log.debug("Checking if category exists: {}", categoryId);

		return webClientBuilder.baseUrl(productServiceUrl)
				.build()
				.get()
				.uri("/api/v1/categories/{id}", categoryId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(CategoryResponse.class)
				.map(response -> true)
				.onErrorReturn(false);
	}

	public Mono<Boolean> categoryExistsFallback(String categoryId, Exception e) {
		log.warn("Using fallback for category exists. Error: {}", e.getMessage());
		return Mono.just(true);
	}

	@CircuitBreaker(name = "productService", fallbackMethod = "getMultipleCategoriesFallback")
	public Flux<CategoryResponse> getCategories(List<UUID> categoryIds) {
		log.debug("Fetching multiple category details for IDs: {}", categoryIds);

		return webClientBuilder.baseUrl(productServiceUrl)
				.build()
				.get()
				.uri(uriBuilder -> uriBuilder.path("/api/v1/categories")
						.queryParam("ids", (Object[]) categoryIds.stream().map(UUID::toString).toArray(String[]::new))
						.build())
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToFlux(CategoryResponse.class)
				.doOnError(e -> log.error("Error fetching multiple category details: {}", e.getMessage(), e));
	}

	public Flux<CategoryResponse> getMultipleCategoriesFallback(List<UUID> categoryIds, Exception e) {
		log.warn("Using fallback for multiple category details. Error: {}", e.getMessage());
		return Flux.fromIterable(categoryIds)
				.map(id -> new CategoryResponse(id, "Unknown Category", "Category information unavailable"));
	}

	@Builder
	public record CategoryResponse(
			UUID id,
			String name,
			String description
	) {
	}
}
