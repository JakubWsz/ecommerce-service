package pl.ecommerce.vendor.read.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.vendor.read.application.dto.AddressDto;
import pl.ecommerce.vendor.read.application.dto.BankDetailsDto;
import pl.ecommerce.vendor.read.application.dto.CategoryDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@Schema(description = "Detailed vendor information")
public class VendorResponse {

	@Schema(description = "Unique identifier of the vendor", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
	private UUID id;

	@Schema(description = "Vendor's name", example = "Tech Solutions Inc")
	private String name;

	@Schema(description = "Vendor's business name", example = "Tech Solutions Inc Ltd.")
	private String businessName;

	@Schema(description = "Vendor's tax ID", example = "TAX123456789")
	private String taxId;

	@Schema(description = "Vendor's email address", example = "contact@techsolutions.com")
	private String email;

	@Schema(description = "Vendor's phone number", example = "+1234567890")
	private String phone;

	@Schema(description = "Legal form of the business", example = "LLC")
	private String legalForm;

	@Schema(description = "Vendor status", example = "ACTIVE")
	private VendorStatus status;

	@Schema(description = "Indicates if the vendor is verified", example = "true")
	private boolean verified;

	@Schema(description = "Commission rate applied to vendor's sales", example = "15.0")
	private BigDecimal commissionRate;

	@Schema(description = "Contact person's name", example = "John Smith")
	private String contactPersonName;

	@Schema(description = "Contact person's email", example = "john.smith@techsolutions.com")
	private String contactPersonEmail;

	@Schema(description = "Timestamp when the vendor was created", example = "2023-01-01T12:00:00Z")
	private Instant createdAt;

	@Schema(description = "Timestamp when the vendor was last updated", example = "2023-01-02T12:00:00Z")
	private Instant updatedAt;

	@Schema(description = "Bank account details of the vendor")
	private BankDetailsDto bankDetails;

	@Schema(description = "List of product categories assigned to the vendor")
	private List<CategoryDto> categories;

	@Schema(description = "Vendor's address")
	private AddressDto address;

	@Schema(description = "Trace identifier", example = "12345678-1234-1234-1234-1234567890ab")
	private String traceId;
}