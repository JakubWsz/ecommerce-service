package pl.ecommerce.vendor.infrastructure.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "pl.ecommerce.vendor.domain.repository")
@EnableReactiveMongoAuditing
@EnableTransactionManagement
@EnableConfigurationProperties(MongoProperties.class)
public class MongoConfig {

	@Bean
	public MongoClient reactiveMongoClient(MongoProperties properties) {
		return MongoClients.create(
				MongoClientSettings.builder()
						.applyConnectionString(new com.mongodb.ConnectionString(properties.getUri()))
						.uuidRepresentation(UuidRepresentation.STANDARD)
						.build()
		);
	}

	@Bean
	public ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoClient mongoClient, MongoProperties properties) {
		return new SimpleReactiveMongoDatabaseFactory(mongoClient, properties.getDatabase());
	}

	@Bean
	public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory factory, MongoCustomConversions conversions) {
		ReactiveMongoTemplate template = new ReactiveMongoTemplate(factory);
		template.setWriteConcern(com.mongodb.WriteConcern.MAJORITY);
		return template;
	}

	@Bean
	public ReactiveTransactionManager reactiveTransactionManager(ReactiveMongoDatabaseFactory databaseFactory) {
		return new ReactiveMongoTransactionManager(databaseFactory);
	}
}
