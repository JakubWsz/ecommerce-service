package pl.ecommerce.vendor.write.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vendor registration request")
public class VendorRegistrationRequest {

	@NotBlank(message = "Name is required")
	@Schema(description = "Vendor's name", example = "Tech Solutions Inc", required = true)
	private String name;

	@NotBlank(message = "Business name is required")
	@Schema(description = "Vendor's business name", example = "Tech Solutions Inc Ltd.", required = true)
	private String businessName;

	@NotBlank(message = "Tax ID is required")
	@Schema(description = "Vendor's tax ID", example = "TAX123456789", required = true)
	private String taxId;

	@NotBlank(message = "Email is required")
	@Email(message = "Email should be valid")
	@Schema(description = "Vendor's email address", example = "contact@techsolutions.com", required = true)
	private String email;

	@Schema(description = "Vendor's phone number", example = "+1234567890")
	private String phone;

	@Schema(description = "Legal form of the business", example = "LLC")
	private String legalForm;

	@Schema(description = "Initial product categories assigned to the vendor")
	private Set<UUID> initialCategories;

	@NotNull(message = "Commission rate is required")
	@Schema(description = "Commission rate applied to vendor's sales", example = "15.0", required = true)
	private BigDecimal commissionRate;

	@Schema(description = "Contact person's name", example = "John Smith")
	private String contactPersonName;

	@Schema(description = "Contact person's email", example = "john.smith@techsolutions.com")
	private String contactPersonEmail;
}