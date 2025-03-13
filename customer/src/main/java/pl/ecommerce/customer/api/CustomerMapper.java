package pl.ecommerce.customer.api;

import pl.ecommerce.customer.api.dto.*;
import pl.ecommerce.customer.aplication.dto.AddressDto;
import pl.ecommerce.customer.aplication.dto.CustomerReadModel;
import pl.ecommerce.customer.aplication.dto.CustomerResponseDto;
import pl.ecommerce.customer.domain.valueobjects.CustomerPreferences;

import java.util.ArrayList;


public interface CustomerMapper {

	static CustomerResponseDto toResponseDto(CustomerReadModel readModel) {
		return CustomerResponseDto.builder()
				.id(readModel.getId())
				.email(readModel.getEmail())
				.firstName(readModel.getFirstName())
				.lastName(readModel.getLastName())
				.phoneNumber(readModel.getPhoneNumber())
				.emailVerified(readModel.isEmailVerified())
				.status(readModel.getStatus().name())
				.createdAt(readModel.getCreatedAt())
				.updatedAt(readModel.getUpdatedAt())
				.addresses(readModel.getAddresses())
				.geoLocationData(readModel.getGeoLocationData())
				.preferences(readModel.getPreferences())
				.build();
	}

	static CustomerPreferences toPreferencesDomain(PreferencesRequest request) {
		return CustomerPreferences.builder()
				.marketingConsent(request.marketingConsent())
				.newsletterSubscribed(request.newsletterSubscribed())
				.preferredLanguage(request.preferredLanguage())
				.preferredCurrency(request.preferredCurrency())
				.favoriteCategories(request.favoriteCategories() != null ?
						new ArrayList<>(request.favoriteCategories()) : new ArrayList<>())
				.build();
	}

	static pl.ecommerce.customer.aplication.dto.AddressDto toAddressResponseDto(AddressDto addressDto) {
		return pl.ecommerce.customer.aplication.dto.AddressDto.builder()
				.id(addressDto.getId())
				.street(addressDto.getStreet())
				.buildingNumber(addressDto.getBuildingNumber())
				.apartmentNumber(addressDto.getApartmentNumber())
				.city(addressDto.getCity())
				.state(addressDto.getState())
				.postalCode(addressDto.getPostalCode())
				.country(addressDto.getCountry())
				.isDefault(addressDto.isDefault())
				.addressType(addressDto.getAddressType())
				.build();
	}
}
