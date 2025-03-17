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
@Schema(description = "Request to update vendor's bank details")
public class UpdateBankDetailsRequest {

	@NotBlank(message = "Bank account number is required")
	@Schema(description = "Bank account number", example = "PL61109010140000071219812874", required = true)
	private String bankAccountNumber;

	@NotBlank(message = "Bank name is required")
	@Schema(description = "Name of the bank", example = "International Bank", required = true)
	private String bankName;

	@Schema(description = "SWIFT/BIC code of the bank", example = "BREXPLPWXXX")
	private String bankSwiftCode;
}