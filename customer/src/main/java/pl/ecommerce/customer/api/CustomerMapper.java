package pl.ecommerce.customer.api;

import pl.ecommerce.customer.api.dto.*;
import pl.ecommerce.customer.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;


public final class CustomerMapper {

	private CustomerMapper() {
	}

	public static CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.isActive(),
				customer.getRegistrationIp(),
				customer.getCreatedAt().toString(),
				customer.getUpdatedAt().toString(),
				customer.getPersonalData() != null ? toPersonalDataDto(customer.getPersonalData()) : null,
				customer.getAddresses() != null
						? customer.getAddresses().stream().map(CustomerMapper::toAddressDto).toList()
						: List.of(),
				customer.getGeoLocationData() != null ? toGeoLocationDataDto(customer.getGeoLocationData()) : null
		);
	}

	public static PersonalDataDto toPersonalDataDto(PersonalData pd) {
		return new PersonalDataDto(pd.getEmail(), pd.getFirstName(), pd.getLastName(), pd.getPhoneNumber());
	}

	public static AddressDto toAddressDto(Address addr) {
		return new AddressDto(
				addr.getStreet(),
				addr.getBuildingNumber(),
				addr.getApartmentNumber(),
				addr.getCity(),
				addr.getState(),
				addr.getPostalCode(),
				addr.getCountry(),
				addr.isDefault(),
				addr.getAddressType().name()
		);
	}

	public static GeoLocationDataDto toGeoLocationDataDto(GeoLocationData geo) {
		return new GeoLocationDataDto(
				geo.getCountry(),
				geo.getCity(),
				geo.getVoivodeship(),
				geo.getPostalCode()
		);
	}

	public static Customer toCustomer(CustomerRequest request) {
		PersonalData pd = toPersonalData(request.personalData());
		List<Address> addresses = request.addresses().stream()
				.map(CustomerMapper::toAddress)
				.toList();

		return new Customer(
				true,
				"",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				request.personalData() != null,
				LocalDateTime.now(),
				false,
				false,
				0,
				pd,
				addresses,
				null,
				List.of()
		);
	}

	private static PersonalData toPersonalData(PersonalDataDto dto) {
		if (dto == null) {
			return null;
		}
		return new PersonalData(
				dto.email(),
				dto.firstName(),
				dto.lastName(),
				dto.phoneNumber()
		);
	}

	private static Address toAddress(AddressDto dto) {
		return new Address(
				dto.street(),
				dto.buildingNumber(),
				dto.apartmentNumber(),
				dto.city(),
				dto.state(),
				dto.postalCode(),
				dto.country(),
				dto.isDefault(),
				AddressType.valueOf(dto.addressType())
		);
	}
}