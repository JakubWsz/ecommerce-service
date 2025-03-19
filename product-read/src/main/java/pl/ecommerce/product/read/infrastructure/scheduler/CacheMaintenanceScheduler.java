package pl.ecommerce.product.read.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.ecommerce.product.read.application.service.ProductCacheService;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import pl.ecommerce.product.read.infrastructure.repository.ProductReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Scheduler do okresowego czyszczenia nieaktualnych elementów z cache
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheMaintenanceScheduler {

	private final ReactiveRedisTemplate<String, ProductReadModel> productRedisTemplate;
	private final ProductCacheService cacheService;
	private final ProductReadRepository productRepository;

	private static final String PRODUCT_KEY_PATTERN = "product:*";
	private static final int BATCH_SIZE = 100;

	/**
	 * Usuwa z cache produkty oznaczone jako niedostępne lub usunięte
	 * Uruchamiane co 30 minut
	 */
	@Scheduled(fixedDelayString = "${cache.maintenance.interval:1800000}")
	public void removeOutOfStockProducts() {
		log.info("Starting scheduled cache maintenance - removing out of stock products");

		// Lista kluczy do usunięcia
		List<String> keysToRemove = new ArrayList<>();

		// Skanowanie kluczy w Redis
		productRedisTemplate.scan(ScanOptions.scanOptions()
						.match(PRODUCT_KEY_PATTERN)
						.count(BATCH_SIZE)
						.build())
				.map(key -> key.replaceFirst("product:", ""))
				.filter(key -> {
					try {
						// Sprawdź czy klucz to UUID
						UUID.fromString(key);
						return true;
					} catch (IllegalArgumentException e) {
						return false;
					}
				})
				.flatMap(key -> {
					UUID productId = UUID.fromString(key);

					// Sprawdź aktualny stan produktu w bazie danych
					return productRepository.findById(productId)
							.defaultIfEmpty(null)
							.map(product -> {
								// Jeśli produkt nie istnieje w bazie, oznacz do usunięcia
								if (Objects.isNull(product)) {
									return "product:" + key;
								}

								// Jeśli produkt jest niedostępny lub usunięty, oznacz do usunięcia
								if (Objects.nonNull(product.getStatus()) &&
										("DELETED".equals(product.getStatus()) ||
												"OUT_OF_STOCK".equals(product.getStatus()))) {
									return "product:" + key;
								}

								// Jeśli produkt nie ma zapasów, oznacz do usunięcia
								if (Objects.nonNull(product.getStock()) &&
										product.getStock().getAvailable() <= 0) {
									return "product:" + key;
								}

								// W przeciwnym razie nie usuwaj
								return null;
							});
				})
				.filter(Objects::nonNull)
				.doOnNext(keysToRemove::add)
				.then()
				.doOnSuccess(v -> {
					log.info("Found {} outdated product keys to remove from cache", keysToRemove.size());

					if (!keysToRemove.isEmpty()) {
						productRedisTemplate.delete(keysToRemove)
								.subscribe(
										count -> log.info("Removed {} outdated keys from Redis cache", count),
										error -> log.error("Error removing keys from Redis cache", error)
								);
					}
				})
				.doOnError(error -> log.error("Error during cache maintenance", error))
				.subscribe();
	}

	/**
	 * Odświeża w cache produkty oznaczone jako popularne/wyróżnione
	 * Uruchamiane co 10 minut
	 */
	@Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
	public void refreshFeaturedProducts() {
		log.info("Starting scheduled cache refresh - refreshing featured products");

		// Pobierz wyróżnione produkty z bazy danych
		productRepository.findByFeaturedTrue(org.springframework.data.domain.PageRequest.of(0, 20))
				.flatMap(product -> {
					// Odśwież produkt w cache
					return cacheService.cacheProduct(product)
							.doOnSuccess(result -> log.debug("Refreshed featured product in cache: {}", product.getId()));
				})
				.collectList()
				.doOnSuccess(list -> log.info("Refreshed {} featured products in cache", list.size()))
				.doOnError(error -> log.error("Error refreshing featured products in cache", error))
				.subscribe();
	}
}