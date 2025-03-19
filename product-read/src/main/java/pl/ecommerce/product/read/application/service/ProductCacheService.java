package pl.ecommerce.product.read.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import pl.ecommerce.product.read.api.dto.ProductResponse;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.application.mapper.ProductMapper;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

	private static final String PRODUCT_KEY_PREFIX = "product:";
	private static final String PRODUCT_SUMMARY_KEY_PREFIX = "product:summary:";
	private static final String PRODUCT_RESPONSE_KEY_PREFIX = "product:response:";
	private static final String PRODUCT_SKU_KEY_PREFIX = "product:sku:";

	private static final Duration DEFAULT_CACHE_DURATION = Duration.ofMinutes(30);

	private final ReactiveRedisTemplate<String, ProductReadModel> productRedisTemplate;
	private final ReactiveRedisTemplate<String, ProductResponse> productResponseRedisTemplate;
	private final ReactiveRedisTemplate<String, ProductSummary> productSummaryRedisTemplate;

	public Mono<ProductReadModel> getProduct(UUID productId) {
		String key = PRODUCT_KEY_PREFIX + productId.toString();
		return productRedisTemplate.opsForValue().get(key);
	}

	public Mono<ProductReadModel> getProductBySku(String sku) {
		String key = PRODUCT_SKU_KEY_PREFIX + sku;
		return productRedisTemplate.opsForValue().get(key);
	}

	public Mono<Boolean> cacheProduct(ProductReadModel product) {
		if (Objects.isNull(product) || Objects.isNull(product.getId())) {
			return Mono.just(false);
		}

		String key = PRODUCT_KEY_PREFIX + product.getId().toString();
		String skuKey = PRODUCT_SKU_KEY_PREFIX + product.getSku();

		return productRedisTemplate.opsForValue()
				.set(key, product, DEFAULT_CACHE_DURATION)
				.flatMap(result -> productRedisTemplate.opsForValue()
						.set(skuKey, product, DEFAULT_CACHE_DURATION));
	}

	// Metody dla ProductResponse

	public Mono<ProductResponse> getProductResponse(UUID productId) {
		String key = PRODUCT_RESPONSE_KEY_PREFIX + productId.toString();
		return productResponseRedisTemplate.opsForValue().get(key);
	}

	public Mono<Boolean> cacheProductResponse(ProductResponse productResponse) {
		if (Objects.isNull(productResponse) || Objects.isNull(productResponse.getId())) {
			return Mono.just(false);
		}

		String key = PRODUCT_RESPONSE_KEY_PREFIX + productResponse.getId().toString();
		return productResponseRedisTemplate.opsForValue().set(key, productResponse, DEFAULT_CACHE_DURATION);
	}

	// Metody dla ProductSummary

	public Mono<ProductSummary> getProductSummary(UUID productId) {
		String key = PRODUCT_SUMMARY_KEY_PREFIX + productId.toString();
		return productSummaryRedisTemplate.opsForValue().get(key);
	}

	public Mono<Boolean> cacheProductSummary(ProductSummary productSummary) {
		if (Objects.isNull(productSummary) || Objects.isNull(productSummary.getId())) {
			return Mono.just(false);
		}

		String key = PRODUCT_SUMMARY_KEY_PREFIX + productSummary.getId().toString();
		return productSummaryRedisTemplate.opsForValue().set(key, productSummary, DEFAULT_CACHE_DURATION);
	}

	// Metody do usuwania z cache'a

	public Mono<Boolean> removeProduct(UUID productId) {
		if (Objects.isNull(productId)) {
			return Mono.just(false);
		}

		String key = PRODUCT_KEY_PREFIX + productId.toString();
		String responseKey = PRODUCT_RESPONSE_KEY_PREFIX + productId.toString();
		String summaryKey = PRODUCT_SUMMARY_KEY_PREFIX + productId.toString();

		return productRedisTemplate.delete(key)
				.then(productResponseRedisTemplate.delete(responseKey))
				.then(productSummaryRedisTemplate.delete(summaryKey))
				.map(result -> true)
				.onErrorReturn(false);
	}

	public Mono<Boolean> removeProductBySku(String sku) {
		if (Objects.isNull(sku) || sku.isEmpty()) {
			return Mono.just(false);
		}

		String skuKey = PRODUCT_SKU_KEY_PREFIX + sku;

		return getProductBySku(sku)
				.flatMap(product -> {
					if (Objects.nonNull(product)) {
						return removeProduct(product.getId())
								.then(productRedisTemplate.delete(skuKey))
								.map(result -> true);
					}
					return productRedisTemplate.delete(skuKey).map(result -> true);
				})
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> removeOutOfStockProduct(UUID productId) {
		return getProduct(productId)
				.flatMap(product -> {
					if (Objects.nonNull(product) && Objects.nonNull(product.getStock()) &&
							!product.getStock().isInStock()) {
						return removeProduct(productId);
					}
					return Mono.just(false);
				})
				.defaultIfEmpty(false);
	}

	// Metoda do aktualizacji produktu w cache'u

	public Mono<Boolean> updateProductStock(UUID productId, int newQuantity, int reservedQuantity) {
		return getProduct(productId)
				.flatMap(product -> {
					if (Objects.isNull(product) || Objects.isNull(product.getStock())) {
						return Mono.just(false);
					}

					// Aktualizacja stanu magazynowego
					product.getStock().setAvailable(newQuantity);
					product.getStock().setReserved(reservedQuantity);

					// Jeśli produkt jest niedostępny, usuń z cache'a
					if (newQuantity <= 0) {
						return removeProduct(productId);
					}

					// W przeciwnym razie zaktualizuj cache
					return cacheProduct(product)
							.then(getProductResponse(productId)
									.flatMap(response -> {
										// Aktualizacja odpowiedzi
										response.getStock().setAvailable(newQuantity);
										response.getStock().setReserved(reservedQuantity);
										response.getStock().setInStock(newQuantity > 0);
										response.getStock().setLowStock(newQuantity > 0 && newQuantity <= 5);
										return cacheProductResponse(response);
									})
									.defaultIfEmpty(true))
							.then(getProductSummary(productId)
									.flatMap(summary -> {
										// Aktualizacja podsumowania
										summary.setInStock(newQuantity > 0);
										return cacheProductSummary(summary);
									})
									.defaultIfEmpty(true));
				})
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> refreshProduct(UUID productId) {
		if (Objects.isNull(productId)) {
			return Mono.just(false);
		}

		return productRepository.findById(productId)
				.flatMap(product -> {
					// Usuń stare wersje z cache
					return removeProduct(productId)
							.then(cacheProduct(product));
				})
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> refreshFeaturedProducts() {
		return productRepository.findByFeaturedTrue(org.springframework.data.domain.PageRequest.of(0, 50))
				.flatMap(product -> cacheProduct(product))
				.collectList()
				.map(results -> !results.isEmpty())
				.defaultIfEmpty(false);
	}

	public Mono<Boolean> clearAllCache() {
		return productRedisTemplate.delete(PRODUCT_KEY_PREFIX + "*")
				.then(productResponseRedisTemplate.delete(PRODUCT_RESPONSE_KEY_PREFIX + "*"))
				.then(productSummaryRedisTemplate.delete(PRODUCT_SUMMARY_KEY_PREFIX + "*"))
				.then(productRedisTemplate.delete(PRODUCT_SKU_KEY_PREFIX + "*"))
				.map(totalDeleted -> totalDeleted > 0)
				.defaultIfEmpty(true);
	}

	public Mono<CacheStatistics> getCacheStatistics() {
		CacheStatistics stats = new CacheStatistics();

		// Pobieranie liczby trafień i braków z metryk cache
		// Te wartości najlepiej byłoby pobierać z faktycznych liczników
		// które są inkrementowane przy każdym dostępie do cache
		return Mono.fromSupplier(() -> {
					stats.setHits(metricRegistry.counter("cache.hits").count());
					stats.setMisses(metricRegistry.counter("cache.misses").count());

					long total = stats.getHits() + stats.getMisses();
					stats.setHitRatio(total > 0 ? (double) stats.getHits() / total : 0);

					return stats;
				})
				.then(productRedisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
								.match(PRODUCT_KEY_PREFIX + "*")
								.build())
						.count()
						.doOnNext(count -> stats.setSize(count)))
				.then(productRedisTemplate.getConnectionFactory().getReactiveConnection()
						.serverCommands()
						.info("memory")
						.doOnNext(info -> {
							// Parsowanie odpowiedzi info, aby uzyskać użycie pamięci
							// Przykładowe parsowanie - w rzeczywistym kodzie należy dostosować
							// do faktycznego formatu odpowiedzi Redis
							String memoryLine = info.toString();
							int usedMemoryIndex = memoryLine.indexOf("used_memory:");
							if (usedMemoryIndex >= 0) {
								String memorySubstring = memoryLine.substring(usedMemoryIndex + 12);
								int endIndex = memorySubstring.indexOf("\r\n");
								if (endIndex >= 0) {
									memorySubstring = memorySubstring.substring(0, endIndex);
								}
								try {
									stats.setMemoryUsage(Long.parseLong(memorySubstring.trim()));
								} catch (NumberFormatException e) {
									stats.setMemoryUsage(-1); // Nie udało się sparsować
								}
							}
						}))
				.thenReturn(stats);
	}
}