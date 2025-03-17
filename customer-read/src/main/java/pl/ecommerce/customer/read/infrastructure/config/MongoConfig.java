//package pl.ecommerce.customer.read.infrastructure.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
//
//import java.util.Arrays;
//import java.util.UUID;
//
//@Configuration
//public class MongoConfig {
//	@Bean
//	public MongoCustomConversions mongoCustomConversions() {
//		return new MongoCustomConversions(
//				Arrays.asList(
//						new UuidToStringConverter(),
//						new StringToUuidConverter()
//				)
//		);
//	}
//
//	private static class UuidToStringConverter implements Converter<UUID, String> {
//		@Override
//		public String convert(UUID source) {
//			return source.toString();
//		}
//	}
//
//	private static class StringToUuidConverter implements Converter<String, UUID> {
//		@Override
//		public UUID convert(String source) {
//			return UUID.fromString(source);
//		}
//	}
//}