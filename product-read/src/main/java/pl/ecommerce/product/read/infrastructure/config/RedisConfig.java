package pl.ecommerce.product.read.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ecommerce.product.read.domain.model.ProductReadModel;

import java.time.Duration;

@Configuration
public class RedisConfig {

	@Bean
	public ReactiveRedisTemplate<String, ProductReadModel> reactiveProductRedisTemplate(
			RedisConnectionFactory factory, ObjectMapper objectMapper) {

		objectMapper.registerModule(new JavaTimeModule());
		Jackson2JsonRedisSerializer<ProductReadModel> serializer =
				new Jackson2JsonRedisSerializer<>(ProductReadModel.class);
		serializer.setObjectMapper(objectMapper);

		RedisSerializationContext<String, ProductReadModel> serializationContext =
				RedisSerializationContext.<String, ProductReadModel>newSerializationContext(new StringRedisSerializer())
						.value(serializer)
						.build();

		return new ReactiveRedisTemplate<>(factory, serializationContext);
	}

	@Bean
	public ReactiveRedisTemplate<String, ProductResponse> reactiveProductResponseRedisTemplate(
			RedisConnectionFactory factory, ObjectMapper objectMapper) {

		objectMapper.registerModule(new JavaTimeModule());
		Jackson2JsonRedisSerializer<ProductResponse> serializer =
				new Jackson2JsonRedisSerializer<>(ProductResponse.class);
		serializer.setObjectMapper(objectMapper);

		RedisSerializationContext<String, ProductResponse> serializationContext =
				RedisSerializationContext.<String, ProductResponse>newSerializationContext(new StringRedisSerializer())
						.value(serializer)
						.build();

		return new ReactiveRedisTemplate<>(factory, serializationContext);
	}

	@Bean
	public ReactiveRedisTemplate<String, ProductSummary> reactiveProductSummaryRedisTemplate(
			RedisConnectionFactory factory, ObjectMapper objectMapper) {

		objectMapper.registerModule(new JavaTimeModule());
		Jackson2JsonRedisSerializer<ProductSummary> serializer =
				new Jackson2JsonRedisSerializer<>(ProductSummary.class);
		serializer.setObjectMapper(objectMapper);

		RedisSerializationContext<String, ProductSummary> serializationContext =
				RedisSerializationContext.<String, ProductSummary>newSerializationContext(new StringRedisSerializer())
						.value(serializer)
						.build();

		return new ReactiveRedisTemplate<>(factory, serializationContext);
	}

	@Bean
	public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofHours(1))
				.disableCachingNullValues();

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(cacheConfiguration)
				.withCacheConfiguration("products",
						RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)))
				.withCacheConfiguration("featuredProducts",
						RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15)))
				.build();
	}
}
