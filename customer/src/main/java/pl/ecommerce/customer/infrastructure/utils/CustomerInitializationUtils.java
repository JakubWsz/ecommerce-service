package pl.ecommerce.customer.infrastructure.utils;

import pl.ecommerce.customer.domain.model.Customer;
import pl.ecommerce.customer.domain.model.GeoLocationData;
import pl.ecommerce.customer.infrastructure.client.dto.GeoLocationResponse;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CustomerInitializationUtils {

	private CustomerInitializationUtils() {
	}

	public static Customer initializeNewCustomer(Customer customer, String ipAddress, Integer dataRetentionPeriodDays) {
		LocalDateTime now = LocalDateTime.now();
		customer.setActive(true);
		customer.setRegistrationIp(ipAddress);
		customer.setCreatedAt(now);
		customer.setUpdatedAt(now);
		customer.setConsentTimestamp(now);
		customer.setDataRetentionPeriodDays(dataRetentionPeriodDays);
		return customer;
	}

	public static Customer mapGeoLocationDataToCustomer(Customer customer, GeoLocationResponse geoResponse) {
		customer.setGeoLocationData(new GeoLocationData(
				geoResponse.getCountry(),
				geoResponse.getCity(),
				geoResponse.getState(),
				geoResponse.getPostalCode()
		));

		if (geoResponse.getCurrency() != null && !geoResponse.getCurrency().isEmpty()) {
			List<MonetaryAmount> currencies = geoResponse.getCurrency().entrySet().stream()
					.map(entry -> (MonetaryAmount) Money.of(
							new BigDecimal(entry.getValue()),
							Monetary.getCurrency(entry.getKey())))
					.toList();
			customer.setCurrencies(currencies);
		}

		return customer;
	}

	public static Customer prepareForDeactivation(Customer customer) {
		customer.setActive(false);
		customer.setUpdatedAt(LocalDateTime.now());
		return customer;
	}
}