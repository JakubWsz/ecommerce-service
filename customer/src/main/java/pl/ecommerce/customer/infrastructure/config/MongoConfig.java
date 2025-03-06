package pl.ecommerce.customer.infrastructure.config;

import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "pl.ecommerce.customer.domain.repository")
@EnableReactiveMongoAuditing
@EnableTransactionManagement
@EnableConfigurationProperties(MongoProperties.class)
public class MongoConfig {

	@Bean
	public ReactiveTransactionManager reactiveTransactionManager(ReactiveMongoDatabaseFactory databaseFactory) {
		return new ReactiveMongoTransactionManager(databaseFactory);
	}

	@Bean
	public MappingMongoConverter mappingMongoConverter(MappingMongoConverter converter) {
		converter.setMapKeyDotReplacement("_");
		return converter;
	}
}