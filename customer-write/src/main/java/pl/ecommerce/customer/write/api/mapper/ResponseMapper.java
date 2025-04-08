package pl.ecommerce.customer.write.api.mapper;

import pl.ecommerce.commons.model.customer.CustomerPreferences;
import pl.ecommerce.customer.write.api.dto.*;

import java.util.UUID;

public interface ResponseMapper {

	static CustomerRegistrationResponse map(UUID customerId, CustomerRegistrationRequest request) {
		return new CustomerRegistrationResponse(
				customerId,
				request.email(),
				request.firstName(),
				request.lastName()
		);
	}

	static CustomerUpdateResponse map(UUID customerId, CustomerUpdateRequest request) {
		return new CustomerUpdateResponse(
				customerId,
				request.firstName(),
				request.lastName()
		);
	}

	static CustomerVerificationResponse map(UUID customerId) {
		return new CustomerVerificationResponse(
				customerId
		);
	}

	static CustomerPreferencesResponse map(UUID customerId, UpdatePreferencesRequest request) {
		return new CustomerPreferencesResponse(
				customerId,
				map(request)
		);
	}

	static CustomerPreferences map(UpdatePreferencesRequest request) {
		return CustomerPreferences.builder()
				.preferredLanguage(request.preferredLanguage())
				.favoriteCategories(request.favoriteCategories())
				.marketingConsent(request.marketingConsent())
				.preferredCurrency(request.preferredCurrency())
				.newsletterSubscribed(request.newsletterSubscribed())
				.build();
	}

	static CustomerShippingAddressResponse map(UUID customerId, AddShippingAddressRequest request) {
		return CustomerShippingAddressResponse.builder()
				.customerId(customerId)
				.city(request.city())
				.country(request.country())
				.postalCode(request.postalCode())
				.street(request.street())
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.build();
	}

	static CustomerShippingAddressResponse map(UUID customerId, UpdateShippingAddressRequest request) {
		return CustomerShippingAddressResponse.builder()
				.customerId(customerId)
				.city(request.city())
				.country(request.country())
				.postalCode(request.postalCode())
				.street(request.street())
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.build();
	}
}
