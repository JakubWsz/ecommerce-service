package pl.ecommerce.vendor.write.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to change vendor status")
public class VendorStatusChangeRequest {

	@NotBlank(message = "New status is required")
	@Schema(description = "New status value", example = "ACTIVE", required = true,
			allowableValues = {"PENDING", "ACTIVE", "SUSPENDED", "BANNED"})
	private String newStatus;

	@Schema(description = "Reason for status change", example = "Vendor has completed verification")
	private String reason;
}