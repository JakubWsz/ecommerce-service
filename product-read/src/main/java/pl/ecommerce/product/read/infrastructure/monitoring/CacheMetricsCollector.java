package pl.ecommerce.product.read.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class CacheMetricsCollector {

	private final Counter cacheHitCounter;
	private final Counter cacheMissCounter;
	private final Counter cacheErrorCounter;
	private final Timer cacheAccessTimer;
	private final Timer databaseAccessTimer;

	public CacheMetricsCollector(MeterRegistry registry) {
		this.cacheHitCounter = Counter.builder("cache.hits")
				.description("Liczba trafień w cache")
				.register(registry);

		this.cacheMissCounter = Counter.builder("cache.misses")
				.description("Liczba brakujących elementów w cache")
				.register(registry);

		this.cacheErrorCounter = Counter.builder("cache.errors")
				.description("Liczba błędów cache")
				.register(registry);

		this.cacheAccessTimer = Timer.builder("cache.access.time")
				.description("Czas dostępu do cache")
				.register(registry);

		this.databaseAccessTimer = Timer.builder("database.access.time")
				.description("Czas dostępu do bazy danych")
				.register(registry);
	}

	public void recordCacheHit() {
		cacheHitCounter.increment();
	}

	public void recordCacheMiss() {
		cacheMissCounter.increment();
	}

	public void recordCacheError() {
		cacheErrorCounter.increment();
	}

	public <T> T measureCacheAccess(Supplier<T> operation) {
		return cacheAccessTimer.record(operation);
	}

	public <T> T measureDatabaseAccess(Supplier<T> operation) {
		return databaseAccessTimer.record(operation);
	}

	public void recordCacheStats(long cacheHits, long cacheMisses, long cacheSize) {
		double hitRatio = cacheHits + cacheMisses == 0 ? 0 :
				(double) cacheHits / (cacheHits + cacheMisses);

		log.info("Cache stats - hits: {}, misses: {}, size: {}, hit ratio: {}%",
				cacheHits, cacheMisses, cacheSize, hitRatio * 100);
	}
}