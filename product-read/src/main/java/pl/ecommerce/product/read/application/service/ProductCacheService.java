package pl.ecommerce.product.read.application.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import pl.ecommerce.product.read.api.dto.CacheStatisticsResponse;
import pl.ecommerce.product.read.api.dto.ProductResponse;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import pl.ecommerce.product.read.infrastructure.cache.ReactiveCacheRepository;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static pl.ecommerce.product.read.infrastructure.cahce.CacheKeyGenerator.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

	private static final Duration DEFAULT_CACHE_DURATION = Duration.ofMinutes(30);

	private final ReactiveCacheRepository cacheRepository;
	private final ProductReadRepository productRepository;
	private final MeterRegistry meterRegistry;

	public Mono<ProductReadModel> getProduct(UUID productId) {
		return cacheRepository.getProductRedisTemplate()
				.opsForValue()
				.get(productKey(productId));
	}

	public Mono<ProductReadModel> getProductBySku(String sku) {
		return cacheRepository.getProductRedisTemplate()
				.opsForValue()
				.get(skuKey(sku));
	}

	public Mono<Boolean> cacheProduct(ProductReadModel product) {
		if (product == null || product.getId() == null) {
			return Mono.just(false);
		}
		return cacheRepository.getProductRedisTemplate().opsForValue()
				.set(productKey(product.getId()), product, DEFAULT_CACHE_DURATION)
				.flatMap(result -> cacheRepository.getProductRedisTemplate().opsForValue()
						.set(skuKey(product.getSku()), product, DEFAULT_CACHE_DURATION));
	}

	public Mono<ProductResponse> getProductResponse(UUID productId) {
		return cacheRepository.getProductResponseRedisTemplate()
				.opsForValue()
				.get(productResponseKey(productId));
	}

	public Mono<Boolean> cacheProductResponse(ProductResponse productResponse) {
		if (productResponse == null || productResponse.getId() == null) {
			return Mono.just(false);
		}
		return cacheRepository.getProductResponseRedisTemplate().opsForValue()
				.set(productResponseKey(productResponse.getId()), productResponse, DEFAULT_CACHE_DURATION);
	}

	public Mono<ProductSummary> getProductSummary(UUID productId) {
		return cacheRepository.getProductSummaryRedisTemplate()
				.opsForValue()
				.get(productSummaryKey(productId));
	}

	public Mono<Boolean> cacheProductSummary(ProductSummary productSummary) {
		if (productSummary == null || productSummary.getId() == null) {
			return Mono.just(false);
		}
		return cacheRepository.getProductSummaryRedisTemplate().opsForValue()
				.set(productSummaryKey(productSummary.getId()), productSummary, DEFAULT_CACHE_DURATION);
	}

	public Mono<Boolean> removeProduct(UUID productId) {
		if (productId == null) {
			return Mono.just(false);
		}
		return cacheRepository.getProductRedisTemplate().delete(productKey(productId))
				.then(cacheRepository.getProductResponseRedisTemplate().delete(productResponseKey(productId)))
				.then(cacheRepository.getProductSummaryRedisTemplate().delete(productSummaryKey(productId)))
				.map(result -> true)
				.onErrorReturn(false);
	}

	public Mono<Boolean> removeProductBySku(String sku) {
		if (sku == null || sku.isEmpty()) {
			return Mono.just(false);
		}
		return getProductBySku(sku)
				.flatMap(product -> {
					if (product != null) {
						return removeProduct(product.getId())
								.then(cacheRepository.getProductRedisTemplate().delete(skuKey(sku)))
								.map(result -> true);
					}
					return cacheRepository.getProductRedisTemplate().delete(skuKey(sku)).map(result -> true);
				})
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> removeOutOfStockProduct(UUID productId) {
		return getProduct(productId)
				.flatMap(product -> {
					if (product != null && product.getStock() != null && !product.getStock().isInStock()) {
						return removeProduct(productId);
					}
					return Mono.just(false);
				})
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> updateProductStock(UUID productId, int newQuantity, int reservedQuantity) {
		return getProduct(productId)
				.flatMap(product -> {
					if (product == null || product.getStock() == null) {
						return Mono.just(false);
					}
					product.getStock().setAvailable(newQuantity);
					product.getStock().setReserved(reservedQuantity);

					if (newQuantity <= 0) {
						return removeProduct(productId);
					}

					return cacheProduct(product)
							.then(getProductResponse(productId)
									.flatMap(response -> {
										response.getStock().setAvailable(newQuantity);
										response.getStock().setReserved(reservedQuantity);
										response.getStock().setInStock(newQuantity > 0);
										response.getStock().setLowStock(newQuantity > 0 && newQuantity <= 5);
										return cacheProductResponse(response);
									})
									.defaultIfEmpty(true))
							.then(getProductSummary(productId)
									.flatMap(summary -> {
										summary.setInStock(newQuantity > 0);
										return cacheProductSummary(summary);
									})
									.defaultIfEmpty(true));
				})
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> refreshProduct(UUID productId) {
		if (productId == null) {
			return Mono.just(false);
		}
		return productRepository.findById(productId)
				.flatMap(product -> removeProduct(productId).then(cacheProduct(product)))
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> refreshFeaturedProducts() {
		return productRepository.findByFeaturedTrue(PageRequest.of(0, 50))
				.flatMap(this::cacheProduct)
				.collectList()
				.map(results -> !results.isEmpty())
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> clearAllCache() {
		return cacheRepository.getProductRedisTemplate().delete(productKey(null) + "*")
				.then(cacheRepository.getProductResponseRedisTemplate().delete(productResponseKey(null) + "*"))
				.then(cacheRepository.getProductSummaryRedisTemplate().delete(productSummaryKey(null) + "*"))
				.then(cacheRepository.getProductRedisTemplate().delete(skuKey(null) + "*"))
				.map(totalDeleted -> totalDeleted > 0)
				.defaultIfEmpty(true);
	}

	public Mono<CacheStatisticsResponse> getCacheStatistics() {
		CacheStatisticsResponse stats = new CacheStatisticsResponse();
		return Mono.fromSupplier(() -> {
					try {
						stats.setHits((long) meterRegistry.counter("cache.product.hits").count());
					} catch (Exception e) {
						stats.setHits(0);
						log.debug("Counter cache.product.hits not found");
					}
					try {
						stats.setMisses((long) meterRegistry.counter("cache.product.misses").count());
					} catch (Exception e) {
						stats.setMisses(0);
						log.debug("Counter cache.product.misses not found");
					}
					long total = stats.getHits() + stats.getMisses();
					stats.setHitRatio(total > 0 ? (double) stats.getHits() / total : 0);
					return stats;
				})
				.flatMap(s -> cacheRepository.getProductRedisTemplate().scan(
								ScanOptions.scanOptions().match("product:*").build())
						.count()
						.doOnNext(s::setSize)
						.thenReturn(s))
				.flatMap(s -> cacheRepository.getProductRedisTemplate().getConnectionFactory().getReactiveConnection()
						.serverCommands()
						.info("memory")
						.doOnNext(properties -> {
							String usedMemoryStr = properties.getProperty("used_memory");
							if (usedMemoryStr != null) {
								try {
									stats.setMemoryUsage(Long.parseLong(usedMemoryStr.trim()));
								} catch (NumberFormatException e) {
									stats.setMemoryUsage(-1);
									log.debug("Could not parse memory usage: {}", usedMemoryStr);
								}
							} else {
								log.debug("used_memory property not found in Redis info");
								stats.setMemoryUsage(-1);
							}
						})
						.thenReturn(s))
				.doOnError(e -> log.error("Error getting cache statistics: {}", e.getMessage(), e));
	}


	public Mono<Page<ProductSummary>> getSearchResults(String query, Pageable pageable) {
		if (query == null || query.trim().isEmpty()) {
			return Mono.empty();
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.get(searchResultsKey(query, pageable));
	}

	public Mono<Boolean> cacheSearchResults(String query, Pageable pageable, Page<ProductSummary> results) {
		if (query == null || query.trim().isEmpty() || results == null) {
			return Mono.just(false);
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.set(searchResultsKey(query, pageable), results, DEFAULT_CACHE_DURATION);
	}

	public Mono<Page<ProductSummary>> getCategoryProducts(UUID categoryId, Pageable pageable) {
		if (categoryId == null) {
			return Mono.empty();
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.get(categoryProductsKey(categoryId, pageable));
	}

	public Mono<Boolean> cacheCategoryProducts(UUID categoryId, Pageable pageable, Page<ProductSummary> results) {
		if (categoryId == null || results == null) {
			return Mono.just(false);
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.set(categoryProductsKey(categoryId, pageable), results, DEFAULT_CACHE_DURATION);
	}

	public Mono<Page<ProductSummary>> getVendorProducts(UUID vendorId, Pageable pageable) {
		if (vendorId == null) {
			return Mono.empty();
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.get(vendorProductsKey(vendorId, pageable));
	}

	public Mono<Boolean> cacheVendorProducts(UUID vendorId, Pageable pageable, Page<ProductSummary> results) {
		if (vendorId == null || results == null) {
			return Mono.just(false);
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.set(vendorProductsKey(vendorId, pageable), results, DEFAULT_CACHE_DURATION);
	}

	public Mono<Page<ProductSummary>> getFeaturedProducts(Pageable pageable) {
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.get(featuredProductsKey(pageable));
	}

	public Mono<Boolean> cacheFeaturedProducts(Pageable pageable, Page<ProductSummary> results) {
		if (results == null) {
			return Mono.just(false);
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.set(featuredProductsKey(pageable), results, DEFAULT_CACHE_DURATION);
	}

	public Mono<Page<ProductSummary>> getFilteredProducts(String cacheKey) {
		if (cacheKey == null || cacheKey.isEmpty()) {
			return Mono.empty();
		}
		return cacheRepository.getPageRedisTemplate().opsForValue().get(cacheKey);
	}

	public Mono<Boolean> cacheFilteredProducts(String cacheKey, Page<ProductSummary> results) {
		if (cacheKey == null || cacheKey.isEmpty() || results == null) {
			return Mono.just(false);
		}
		return cacheRepository.getPageRedisTemplate().opsForValue()
				.set(cacheKey, results, DEFAULT_CACHE_DURATION);
	}

	public Flux<ProductSummary> getRelatedProducts(UUID productId, int limit) {
		if (productId == null) {
			return Flux.empty();
		}
		return cacheRepository.getListRedisTemplate().opsForValue()
				.get(relatedProductsKey(productId, limit))
				.flatMapMany(Flux::fromIterable)
				.switchIfEmpty(Flux.empty());
	}

	public Mono<Boolean> cacheRelatedProducts(UUID productId, int limit, List<ProductSummary> results) {
		if (productId == null || results == null) {
			return Mono.just(false);
		}
		return cacheRepository.getListRedisTemplate().opsForValue()
				.set(relatedProductsKey(productId, limit), results, DEFAULT_CACHE_DURATION);
	}
}
