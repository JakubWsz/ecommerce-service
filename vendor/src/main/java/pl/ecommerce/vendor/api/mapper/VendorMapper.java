package pl.ecommerce.vendor.api.mapper;

import pl.ecommerce.vendor.api.dto.AddressDto;
import pl.ecommerce.vendor.api.dto.VendorRequest;
import pl.ecommerce.vendor.api.dto.VendorResponse;
import pl.ecommerce.vendor.api.dto.VendorUpdateRequest;
import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Vendor;

public class VendorMapper {

	private VendorMapper() {
	}

	public static Vendor toVendor(VendorRequest request) {
		return Vendor.builder()
				.name(request.name())
				.description(request.description())
				.email(request.email())
				.phone(request.phone())
				.businessName(request.businessName())
				.taxId(request.taxId())
				.businessAddress(toAddress(request.businessAddress()))
				.bankAccountDetails(request.bankAccountDetails())
				.gdprConsent(request.gdprConsent())
				.build();
	}

	public static Vendor toVendor(VendorUpdateRequest request) {
		return Vendor.builder()
				.name(request.name())
				.description(request.description())
				.email(request.email())
				.phone(request.phone())
				.businessName(request.businessName())
				.taxId(request.taxId())
				.businessAddress(toAddress(request.businessAddress()))
				.bankAccountDetails(request.bankAccountDetails())
				.build();
	}

	public static VendorResponse toResponse(Vendor vendor) {
		return VendorResponse.builder()
				.name(vendor.getName())
				.description(vendor.getDescription())
				.email(vendor.getEmail())
				.phone(vendor.getPhone())
				.businessName(vendor.getBusinessName())
				.taxId(vendor.getTaxId())
				.businessAddress(toAddressDto(vendor.getBusinessAddress()))
				.bankAccountDetails(vendor.getBankAccountDetails())
				.status(String.valueOf(vendor.getVendorStatus()))
				.verificationStatus(String.valueOf(vendor.getVerificationStatus()))
				.commissionRate(vendor.getCommissionRate())
				.createdAt(vendor.getCreatedAt())
				.updatedAt(vendor.getUpdatedAt())
				.active(vendor.getActive())
				.build();
	}

	private static Address toAddress(AddressDto dto) {
		if (dto == null) {
			return null;
		}

		return Address.builder()
				.street(dto.street())
				.city(dto.city())
				.state(dto.state())
				.postalCode(dto.postalCode())
				.country(dto.country())
				.build();
	}

	private static AddressDto toAddressDto(Address address) {
		if (address == null) {
			return null;
		}

		return AddressDto.builder()
				.street(address.getStreet())
				.city(address.getCity())
				.state(address.getState())
				.postalCode(address.getPostalCode())
				.country(address.getCountry())
				.build();
	}
}
