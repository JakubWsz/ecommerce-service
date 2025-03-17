package pl.ecommerce.vendor.write.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify a vendor")
public class VendorVerificationRequest {

	@NotNull(message = "Verification ID is required")
	@Schema(description = "Unique identifier of the verification process",
			example = "d7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a", required = true)
	private UUID verificationId;

	@Schema(description = "List of fields that have been verified",
			example = "[\"identityDocument\", \"businessRegistration\", \"taxCertificate\"]")
	private List<String> verifiedFields;
}