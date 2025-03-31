package pl.ecommerce.customer.read.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;
import java.util.UUID;

@Configuration
public class MongoConfig {

	@Bean
	public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean validator) {
		return new ValidatingMongoEventListener(validator);
	}

	@Bean
	public ReactiveMongoTransactionManager transactionManager(ReactiveMongoDatabaseFactory factory) {
		return new ReactiveMongoTransactionManager(factory);
	}

	@Bean
	public MongoCustomConversions mongoCustomConversions() {
		return new MongoCustomConversions(
				Arrays.asList(
						new UuidToStringConverter())
		);
	}

	private static class UuidToStringConverter implements org.springframework.core.convert.converter.Converter<UUID, String> {
		@Override
		public String convert(UUID source) {
			return source.toString();
		}
	}

//	private static class StringToUuidConverter implements org.springframework.core.convert.converter.Converter<String, UUID> {
//		@Override
//		public UUID convert(String source) {
//			return UUID.fromString(source);
//		}
//	}

//	private static class StringToUuidConverter implements org.springframework.core.convert.converter.Converter<String, UUID> {
//		@Override
//		public UUID convert(String source) {
//			try {
//				return UUID.fromString(source);
//			} catch (IllegalArgumentException e) {
//				// Log error or handle it as required
//				System.err.println("Invalid UUID string: " + source);
//				return null;  // Or consider a default UUID if applicable
//			}
//		}
//	}

//	@Bean
//	public MappingMongoConverter customizeMongoMappingConverter(MappingMongoConverter converter) {
//		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
//		return converter;
//	}
}