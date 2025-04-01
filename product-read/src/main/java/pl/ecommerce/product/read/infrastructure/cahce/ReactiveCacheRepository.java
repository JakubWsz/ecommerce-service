package pl.ecommerce.product.read.infrastructure.cache;

import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import pl.ecommerce.product.read.api.dto.ProductResponse;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Getter
@Repository
public class ReactiveCacheRepository {

	private final ReactiveRedisTemplate<String, ProductReadModel> productRedisTemplate;
	private final ReactiveRedisTemplate<String, ProductResponse> productResponseRedisTemplate;
	private final ReactiveRedisTemplate<String, ProductSummary> productSummaryRedisTemplate;
	private final ReactiveRedisTemplate<String, Page<ProductSummary>> pageRedisTemplate;
	private final ReactiveRedisTemplate<String, List<ProductSummary>> listRedisTemplate;

	public ReactiveCacheRepository(
			ReactiveRedisTemplate<String, ProductReadModel> productRedisTemplate,
			ReactiveRedisTemplate<String, ProductResponse> productResponseRedisTemplate,
			ReactiveRedisTemplate<String, ProductSummary> productSummaryRedisTemplate,
			ReactiveRedisTemplate<String, Page<ProductSummary>> pageRedisTemplate,
			ReactiveRedisTemplate<String, List<ProductSummary>> listRedisTemplate) {
		this.productRedisTemplate = productRedisTemplate;
		this.productResponseRedisTemplate = productResponseRedisTemplate;
		this.productSummaryRedisTemplate = productSummaryRedisTemplate;
		this.pageRedisTemplate = pageRedisTemplate;
		this.listRedisTemplate = listRedisTemplate;
	}

	public <T> Mono<Boolean> setValue(ReactiveRedisTemplate<String, T> template, String key, T value, Duration duration) {
		if (value == null) {
			return Mono.just(false);
		}
		return template.opsForValue().set(key, value, duration);
	}

	public <T> Mono<T> getValue(ReactiveRedisTemplate<String, T> template, String key) {
		return template.opsForValue().get(key);
	}

}
