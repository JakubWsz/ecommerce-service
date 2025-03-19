package pl.ecommerce.product.read.application.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.ProductResponse;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.application.mapper.ProductMapper;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductQueryService {

	private final ProductReadRepository productRepository;
	private final ObservationRegistry observationRegistry;
	private final ProductCacheService cacheService;

	public Mono<ProductResponse> findById(UUID productId, TracingContext tracingContext) {
		log.debug("Finding product by ID: {}, traceId: {}", productId, getTraceId(tracingContext));

		return observe("product.findById",
				// Najpierw sprawdź w cache'u
				cacheService.getProductResponse(productId)
						.switchIfEmpty(
								// Jeśli nie ma w cache'u, pobierz z repozytorium
								productRepository.findById(productId)
										.flatMap(product -> {
											// Zapisz model do cache'a
											cacheService.cacheProduct(product).subscribe();

											// Zbuduj odpowiedź
											ProductResponse response = ProductMapper.toProductResponse(product);
											response.setTraceId(getTraceId(tracingContext));

											// Zapisz odpowiedź do cache'a
											cacheService.cacheProductResponse(response).subscribe();

											return Mono.just(response);
										})
						)
						.doOnNext(response -> {
							// Zawsze ustawiaj aktualny traceId
							if (Objects.nonNull(response)) {
								response.setTraceId(getTraceId(tracingContext));
							}
						})
		);
	}

	public Mono<ProductResponse> findBySku(String sku, TracingContext tracingContext) {
		log.debug("Finding product by SKU: {}, traceId: {}", sku, getTraceId(tracingContext));

		return observe("product.findBySku",
				// Spróbuj znaleźć produkt w cache'u przez SKU
				cacheService.getProductBySku(sku)
						.flatMap(product -> {
							// Jeśli znaleziony w cache'u, użyj cached response lub zbuduj nową
							return cacheService.getProductResponse(product.getId())
									.switchIfEmpty(Mono.defer(() -> {
										ProductResponse response = ProductMapper.toProductResponse(product);
										response.setTraceId(getTraceId(tracingContext));
										cacheService.cacheProductResponse(response).subscribe();
										return Mono.just(response);
									}));
						})
						.switchIfEmpty(
								// Jeśli nie ma w cache'u, pobierz z repozytorium
								productRepository.findBySku(sku)
										.flatMap(product -> {
											// Zapisz model i response do cache'a
											cacheService.cacheProduct(product).subscribe();

											ProductResponse response = ProductMapper.toProductResponse(product);
											response.setTraceId(getTraceId(tracingContext));

											cacheService.cacheProductResponse(response).subscribe();

											return Mono.just(response);
										})
						)
						.doOnNext(response -> {
							// Zawsze ustawiaj aktualny traceId
							if (Objects.nonNull(response)) {
								response.setTraceId(getTraceId(tracingContext));
							}
						})
		);
	}

	public Mono<Page<ProductSummary>> searchProducts(String query, Pageable pageable, TracingContext tracingContext) {
		log.debug("Searching products with query: {}, traceId: {}", query, getTraceId(tracingContext));

		return observe("product.searchProducts",
				productRepository.searchProducts(query, pageable, getTraceId(tracingContext)));
	}

	public Mono<Page<ProductSummary>> findByCategory(UUID categoryId, Pageable pageable, TracingContext tracingContext) {
		log.debug("Finding products by category: {}, traceId: {}", categoryId, getTraceId(tracingContext));

		return observe("product.findByCategory",
				productRepository.findByCategory(categoryId, pageable, getTraceId(tracingContext)));
	}

	public Mono<Page<ProductSummary>> findByVendor(UUID vendorId, Pageable pageable, TracingContext tracingContext) {
		log.debug("Finding products by vendor: {}, traceId: {}", vendorId, getTraceId(tracingContext));

		return observe("product.findByVendor",
				productRepository.findByVendor(vendorId, pageable, getTraceId(tracingContext)));
	}

	public Mono<Page<ProductSummary>> findFeaturedProducts(Pageable pageable, TracingContext tracingContext) {
		log.debug("Finding featured products, traceId: {}", getTraceId(tracingContext));

		return observe("product.findFeatured",
				productRepository.findFeaturedProducts(pageable, getTraceId(tracingContext)));
	}

	public Mono<Page<ProductSummary>> filterProducts(
			Set<UUID> categories,
			Set<UUID> vendors,
			UUID brandId,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean inStock,
			Pageable pageable,
			TracingContext tracingContext) {

		log.debug("Filtering products, traceId: {}", getTraceId(tracingContext));

		return observe("product.filterProducts",
				productRepository.filterProducts(
						categories,
						vendors,
						brandId,
						minPrice,
						maxPrice,
						inStock,
						pageable,
						getTraceId(tracingContext)));
	}

	public Flux<ProductSummary> findRelatedProducts(UUID productId, int limit, TracingContext tracingContext) {
		log.debug("Finding related products for product: {}, traceId: {}", productId, getTraceId(tracingContext));

		return observe("product.findRelated",
				productRepository.findRelatedProducts(productId, limit, getTraceId(tracingContext)));
	}

	private <T> Mono<T> observe(String opName, Mono<T> mono) {
		return Observation.createNotStarted(opName, observationRegistry)
				.observe(() -> mono);
	}

	private <T> Flux<T> observe(String opName, Flux<T> flux) {
		return Observation.createNotStarted(opName, observationRegistry)
				.observe(() -> flux);
	}

	private String getTraceId(TracingContext tracingContext) {
		return Objects.nonNull(tracingContext) ? tracingContext.getTraceId() : "unknown";
	}
}