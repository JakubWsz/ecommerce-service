package pl.ecommerce.product.read.infrastructure.cahce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.ecommerce.product.read.application.service.ProductCacheService;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheMaintenanceScheduler {

	private final ReactiveRedisTemplate<String, ProductReadModel> productRedisTemplate;
	private final ProductCacheService cacheService;
	private final ProductReadRepository productRepository;

	private static final String PRODUCT_KEY_PATTERN = "product:*";
	private static final int BATCH_SIZE = 100;

	@Scheduled(fixedDelayString = "${cache.maintenance.interval:1800000}")
	public void removeOutOfStockProducts() {
		log.info("Starting scheduled cache maintenance - removing out of stock products");

		productRedisTemplate.scan(ScanOptions.scanOptions()
						.match(PRODUCT_KEY_PATTERN)
						.count(BATCH_SIZE)
						.build())
				.map(key -> key.replaceFirst("product:", ""))
				.filter(this::isValidUUID)
				.flatMap(this::processKey)
				.filter(Objects::nonNull)
				.collectList()
				.publishOn(Schedulers.boundedElastic())
				.flatMap(keysToRemove -> {
					log.info("Found {} outdated product keys to remove from cache", keysToRemove.size());
					if (!keysToRemove.isEmpty()) {
						return productRedisTemplate.delete(Flux.fromIterable(keysToRemove))
								.doOnNext(count -> log.info("Removed {} outdated keys from Redis cache", count));
					}
					return Mono.empty();
				})
				.doOnError(error -> log.error("Error during cache maintenance", error))
				.subscribe();
	}

	@Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
	public void refreshFeaturedProducts() {
		log.info("Starting scheduled cache refresh - refreshing featured products");

		productRepository.findByFeaturedTrue(PageRequest.of(0, 20))
				.flatMap(product -> cacheService.cacheProduct(product)
						.doOnSuccess(result -> log.debug("Refreshed featured product in cache: {}", product.getId())))
				.collectList()
				.doOnSuccess(list -> log.info("Refreshed {} featured products in cache", list.size()))
				.doOnError(error -> log.error("Error refreshing featured products in cache", error))
				.subscribe();
	}

	private boolean isValidUUID(String key) {
		try {
			UUID.fromString(key);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private Mono<String> processKey(String key) {
		UUID productId = UUID.fromString(key);
		return productRepository.findById(productId)
				.mapNotNull(product -> {
					if (product.getStatus() != null &&
							("DELETED".equals(product.getStatus()) || "OUT_OF_STOCK".equals(product.getStatus()))) {
						return "product:" + key;
					}
					if (product.getStock() != null && product.getStock().getAvailable() <= 0) {
						return "product:" + key;
					}
					return null;
				})
				.switchIfEmpty(Mono.just("product:" + key))
				.filter(Objects::nonNull);
	}
}