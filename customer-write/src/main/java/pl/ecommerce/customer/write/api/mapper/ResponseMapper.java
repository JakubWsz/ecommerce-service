package pl.ecommerce.customer.write.api.mapper;

import pl.ecommerce.commons.model.customer.CustomerPreferences;
import pl.ecommerce.customer.write.api.dto.*;

import java.util.UUID;

public interface ResponseMapper {

	static CustomerRegistrationResponse map(UUID customerId, CustomerRegistrationRequest request, String traceId) {
		return new CustomerRegistrationResponse(
				customerId,
				request.email(),
				request.firstName(),
				request.lastName(),
				traceId
		);
	}

	static CustomerUpdateResponse map(UUID customerId, CustomerUpdateRequest request, String traceId) {
		return new CustomerUpdateResponse(
				customerId,
				traceId,
				request.firstName(),
				request.lastName()
		);
	}

	static CustomerVerificationResponse map(UUID customerId, String traceId) {
		return new CustomerVerificationResponse(
				customerId,
				traceId
		);
	}

	static CustomerPreferencesResponse map(UUID customerId, UpdatePreferencesRequest request, String traceId) {
		return new CustomerPreferencesResponse(
				customerId,
				map(request),
				traceId
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

	static CustomerShippingAddressResponse map(UUID customerId, AddShippingAddressRequest request, String traceId) {
		return CustomerShippingAddressResponse.builder()
				.customerId(customerId)
				.city(request.city())
				.country(request.country())
				.postalCode(request.postalCode())
				.street(request.street())
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.traceId(traceId)
				.build();
	}

	static CustomerShippingAddressResponse map(UUID customerId, UpdateShippingAddressRequest request, String traceId) {
		return CustomerShippingAddressResponse.builder()
				.customerId(customerId)
				.city(request.city())
				.country(request.country())
				.postalCode(request.postalCode())
				.street(request.street())
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.traceId(traceId)
				.build();
	}
}
