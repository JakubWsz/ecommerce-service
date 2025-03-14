package pl.ecommerce.customer.read.aplication.mapper;

import pl.ecommerce.commons.customer.model.Address;
import pl.ecommerce.commons.customer.model.CustomerPreferences;
import pl.ecommerce.customer.read.aplication.dto.AddressResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerSummary;
import pl.ecommerce.customer.read.aplication.dto.PreferencesResponse;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;

import java.util.Collections;
import java.util.stream.Collectors;

public interface CustomerMapper {

	static CustomerResponse toCustomerResponse(CustomerReadModel readModel) {
		return CustomerResponse.builder()
				.id(readModel.getId())
				.email(readModel.getEmail())
				.firstName(readModel.getFirstName())
				.lastName(readModel.getLastName())
				.phoneNumber(readModel.getPhoneNumber())
				.emailVerified(readModel.isEmailVerified())
				.status(readModel.getStatus().name())
				.createdAt(readModel.getCreatedAt())
				.updatedAt(readModel.getUpdatedAt())
				.addresses(readModel.getAddresses() == null
						? Collections.emptyList()
						: readModel.getAddresses().stream()
						.map(CustomerMapper::map)
						.collect(Collectors.toList()))
				.preferences(readModel.getPreferences() == null
						? null
						: map(readModel.getPreferences()))
				.traceId(readModel.getLastTraceId())
				.build();
	}

	static CustomerSummary toCustomerSummary(CustomerReadModel readModel) {
		return CustomerSummary.builder()
				.id(readModel.getId())
				.firstName(readModel.getFirstName())
				.lastName(readModel.getLastName())
				.email(readModel.getEmail())
				.status(readModel.getStatus())
				.createdAt(readModel.getCreatedAt())
				.addressCount(readModel.getAddresses() == null ? 0 : readModel.getAddresses().size())
				.traceId(readModel.getLastTraceId())
				.build();
	}


	static AddressResponse map(Address address) {
		return AddressResponse.builder()
				.street(address.getStreet())
				.buildingNumber(address.getBuildingNumber())
				.apartmentNumber(address.getApartmentNumber())
				.city(address.getCity())
				.state(address.getState())
				.postalCode(address.getPostalCode())
				.country(address.getCountry())
				.isDefault(address.isDefault())
				.addressType(address.getAddressType().name())
				.build();
	}

	static PreferencesResponse map(CustomerPreferences preferences) {
		return PreferencesResponse.builder()
				.marketingConsent(preferences.isMarketingConsent())
				.newsletterSubscribed(preferences.isNewsletterSubscribed())
				.preferredLanguage(preferences.getPreferredLanguage())
				.preferredCurrency(preferences.getPreferredCurrency())
				.favoriteCategories(preferences.getFavoriteCategories())
				.build();
	}
}
