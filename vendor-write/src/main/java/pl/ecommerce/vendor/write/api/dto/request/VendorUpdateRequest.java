package pl.ecommerce.vendor.write.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vendor update request")
public class VendorUpdateRequest {

	@Schema(description = "Vendor's name", example = "Tech Solutions Inc")
	private String name;

	@Schema(description = "Vendor's business name", example = "Tech Solutions Inc Ltd.")
	private String businessName;

	@Schema(description = "Vendor's phone number", example = "+1234567890")
	private String phone;

	@Schema(description = "Contact person's name", example = "John Smith")
	private String contactPersonName;

	@Email(message = "Email should be valid")
	@Schema(description = "Contact person's email", example = "john.smith@techsolutions.com")
	private String contactPersonEmail;
}