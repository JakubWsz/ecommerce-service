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
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductQueryService {

	private final ProductReadRepository productRepository;
	private final ObservationRegistry observationRegistry;
	private final ProductCacheService cacheService;

	public Mono<ProductResponse> findById(UUID productId, TracingContext tracingContext) {
		return executeWithCache(
				() -> cacheService.getProductResponse(productId),
				() -> productRepository.findById(productId)
						.flatMap(product -> cacheProductAndResponse(product, tracingContext)),
				tracingContext,
				"product.findById"
		);
	}

	public Mono<ProductResponse> findBySku(String sku, TracingContext tracingContext) {
		return executeWithCache(
				() -> cacheService.getProductBySku(sku)
						.flatMap(product -> cacheService.getProductResponse(product.getId())
								.switchIfEmpty(Mono.defer(() -> cacheProductAndResponse(product, tracingContext)))),

				() -> productRepository.findBySku(sku)
						.flatMap(product -> cacheProductAndResponse(product, tracingContext)),

				tracingContext,
				"product.findBySku"
		);
	}

	public Mono<Page<ProductSummary>> searchProducts(String query, Pageable pageable, TracingContext tracingContext) {
		return executeWithCacheForPage(
				() -> cacheService.getSearchResults(query, pageable),
				() -> productRepository.searchProducts(query, pageable, getTraceId(tracingContext))
						.flatMap(results -> cacheService.cacheSearchResults(query, pageable, results)
								.thenReturn(results)),
				tracingContext,
				"product.searchProducts"
		);
	}

	public Mono<Page<ProductSummary>> findByCategory(UUID categoryId, Pageable pageable, TracingContext tracingContext) {
		return executeWithCacheForPage(
				() -> cacheService.getCategoryProducts(categoryId, pageable),
				() -> productRepository.findByCategory(categoryId, pageable, getTraceId(tracingContext))
						.flatMap(results -> cacheService.cacheCategoryProducts(categoryId, pageable, results)
								.thenReturn(results)),
				tracingContext,
				"product.findByCategory"
		);
	}

	public Mono<Page<ProductSummary>> findByVendor(UUID vendorId, Pageable pageable, TracingContext tracingContext) {
		return executeWithCacheForPage(
				() -> cacheService.getVendorProducts(vendorId, pageable),
				() -> productRepository.findByVendor(vendorId, pageable, getTraceId(tracingContext))
						.flatMap(results -> cacheService.cacheVendorProducts(vendorId, pageable, results)
								.thenReturn(results)),
				tracingContext,
				"product.findByVendor"
		);
	}

	public Mono<Page<ProductSummary>> findFeaturedProducts(Pageable pageable, TracingContext tracingContext) {
		return executeWithCacheForPage(
				() -> cacheService.getFeaturedProducts(pageable),
				() -> productRepository.findFeaturedProducts(pageable, getTraceId(tracingContext))
						.flatMap(results -> cacheService.cacheFeaturedProducts(pageable, results)
								.thenReturn(results)),
				tracingContext,
				"product.findFeatured"
		);
	}

	public Mono<Page<ProductSummary>> filterProducts(Set<UUID> categories, Set<UUID> vendors, UUID brandId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, Pageable pageable, TracingContext tracingContext) {
		return executeWithCacheForPage(
				() -> cacheService.getFilteredProducts(cacheKey),
				() -> productRepository.filterProducts(categories, vendors, brandId, minPrice, maxPrice, inStock, pageable, getTraceId(tracingContext))
						.flatMap(results -> cacheService.cacheFilteredProducts(cacheKey, results)
								.thenReturn(results)),
				tracingContext,
				"product.filterProducts"
		);
	}

	public Flux<ProductSummary> findRelatedProducts(UUID productId, int limit, TracingContext tracingContext) {
		return executeWithCacheForFlux(
				() -> cacheService.getRelatedProducts(productId, limit),
				() -> productRepository.findRelatedProducts(productId, limit, getTraceId(tracingContext))
						.collectList()
						.flatMap(results -> cacheService.cacheRelatedProducts(productId, limit, results)
								.thenReturn(results))
						.flatMapMany(Flux::fromIterable),
				tracingContext,
				"product.findRelated"
		);
	}

	private Mono<ProductResponse> executeWithCache(
			Supplier<Mono<ProductResponse>> cacheSupplier,
			Supplier<Mono<ProductResponse>> dbSupplier,
			TracingContext tracingContext,
			String observationName) {

		return observeMono(observationName,
				cacheSupplier.get().switchIfEmpty(dbSupplier.get())
						.doOnNext(response -> response.setTraceId(getTraceId(tracingContext))));
	}

	private Mono<Page<ProductSummary>> executeWithCacheForPage(
			Supplier<Mono<Page<ProductSummary>>> cacheSupplier,
			Supplier<Mono<Page<ProductSummary>>> dbSupplier,
			TracingContext tracingContext,
			String observationName) {

		return observeMono(observationName, cacheSupplier.get()
				.switchIfEmpty(dbSupplier.get())
				.map(results -> applyTraceIdToPage(results, tracingContext)));
	}

	private Flux<ProductSummary> executeWithCacheForFlux(
			Supplier<Flux<ProductSummary>> cacheSupplier,
			Supplier<Flux<ProductSummary>> dbSupplier,
			TracingContext tracingContext,
			String observationName) {

		return observeFlux(observationName, cacheSupplier.get()
				.switchIfEmpty(dbSupplier.get())
				.map(summary -> {
					summary.setTraceId(getTraceId(tracingContext));
					return summary;
				}));
	}

	private Page<ProductSummary> applyTraceIdToPage(Page<ProductSummary> page, TracingContext tracingContext) {
		page.forEach(summary -> summary.setTraceId(getTraceId(tracingContext)));
		return page;
	}

	private Mono<ProductResponse> cacheProductAndResponse(pl.ecommerce.product.read.domain.model.ProductReadModel product, TracingContext tracingContext) {
		ProductResponse response = ProductMapper.toProductResponse(product);
		response.setTraceId(getTraceId(tracingContext));

		return cacheService.cacheProduct(product)
				.then(cacheService.cacheProductResponse(response))
				.thenReturn(response);
	}

	private <T> Mono<T> observeMono(String opName, Mono<T> mono) {
		return Observation.createNotStarted(opName, observationRegistry).observe(() -> mono);
	}

	private <T> Flux<T> observeFlux(String opName, Flux<T> flux) {
		return Observation.createNotStarted(opName, observationRegistry).observe(() -> flux);
	}

	private String getTraceId(TracingContext tracingContext) {
		return nonNull(tracingContext) ? tracingContext.getTraceId() : "unknown";
	}
}
