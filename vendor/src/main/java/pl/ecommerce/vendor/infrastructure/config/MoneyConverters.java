package pl.ecommerce.vendor.infrastructure.config;

import org.javamoney.moneta.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MoneyConverters {

	@Bean
	public MongoCustomConversions mongoCustomConversions() {
		return new MongoCustomConversions(Arrays.asList(
				new MonetaryAmountToMapConverter(),
				new MapToMonetaryAmountConverter()
		));
	}

	@WritingConverter
	public static class MonetaryAmountToMapConverter implements Converter<MonetaryAmount, Map<String, Object>> {
		@Override
		public Map<String, Object> convert(MonetaryAmount source) {
			Map<String, Object> map = new HashMap<>();
			map.put("amount", source.getNumber().numberValue(BigDecimal.class));
			map.put("currency", source.getCurrency().getCurrencyCode());
			return map;
		}
	}

	@ReadingConverter
	public static class MapToMonetaryAmountConverter implements Converter<Map<String, Object>, MonetaryAmount> {
		@Override
		public MonetaryAmount convert(Map<String, Object> source) {
			BigDecimal amount = new BigDecimal(source.get("amount").toString());
			CurrencyUnit currency = Monetary.getCurrency(source.get("currency").toString());
			return Money.of(amount, currency);
		}
	}
}