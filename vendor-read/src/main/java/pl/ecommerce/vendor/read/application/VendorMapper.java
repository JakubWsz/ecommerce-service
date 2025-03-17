package pl.ecommerce.vendor.read.application;

import org.springframework.stereotype.Component;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.vendor.read.api.dto.VendorResponse;
import pl.ecommerce.vendor.read.api.dto.VendorSummary;
import pl.ecommerce.vendor.read.application.dto.AddressDto;
import pl.ecommerce.vendor.read.application.dto.BankDetailsDto;
import pl.ecommerce.vendor.read.application.dto.CategoryDto;
import pl.ecommerce.vendor.read.domain.VendorReadModel;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class VendorMapper {

	public VendorResponse toVendorResponse(VendorReadModel vendor, TracingContext tracingContext) {
		return VendorResponse.builder()
				.id(vendor.getId())
				.name(vendor.getName())
				.businessName(vendor.getBusinessName())
				.taxId(vendor.getTaxId())
				.email(vendor.getEmail())
				.phone(vendor.getPhone())
				.legalForm(vendor.getLegalForm())
				.status(vendor.getStatus())
				.verified(vendor.isVerified())
				.commissionRate(vendor.getCommissionRate())
				.contactPersonName(vendor.getContactPersonName())
				.contactPersonEmail(vendor.getContactPersonEmail())
				.createdAt(vendor.getCreatedAt())
				.updatedAt(vendor.getUpdatedAt())
				.bankDetails(mapBankDetails(vendor.getBankDetails()))
				.categories(mapCategories(vendor.getCategories()))
				.address(mapAddress(vendor.getAddress()))
				.traceId(tracingContext.getTraceId())
				.build();
	}

	public VendorSummary toVendorSummary(VendorReadModel vendor, TracingContext tracingContext) {
		return VendorSummary.builder()
				.id(vendor.getId())
				.name(vendor.getName())
				.businessName(vendor.getBusinessName())
				.email(vendor.getEmail())
				.status(vendor.getStatus())
				.verified(vendor.isVerified())
				.categoryCount(vendor.getCategories() != null ? vendor.getCategories().size() : 0)
				.createdAt(vendor.getCreatedAt())
				.traceId(tracingContext.getTraceId())
				.build();
	}

	private BankDetailsDto mapBankDetails(VendorReadModel.BankDetails bankDetails) {
		if (Objects.isNull(bankDetails)) {
			return null;
		}
		return BankDetailsDto.builder()
				.accountNumber(bankDetails.getAccountNumber())
				.bankName(bankDetails.getBankName())
				.swiftCode(bankDetails.getSwiftCode())
				.build();
	}

	private List<CategoryDto> mapCategories(List<VendorReadModel.CategoryAssignment> categories) {
		if (Objects.isNull(categories)) {
			return List.of();
		}
		return categories.stream()
				.map(category -> CategoryDto.builder()
						.id(category.getCategoryId())
						.name(category.getCategoryName())
						.assignedAt(category.getAssignedAt())
						.build())
				.collect(Collectors.toList());
	}

	private AddressDto mapAddress(VendorReadModel.Address address) {
		if (Objects.isNull(address)) {
			return null;
		}
		return AddressDto.builder()
				.street(address.getStreet())
				.buildingNumber(address.getBuildingNumber())
				.city(address.getCity())
				.postalCode(address.getPostalCode())
				.country(address.getCountry())
				.state(address.getState())
				.build();
	}
}