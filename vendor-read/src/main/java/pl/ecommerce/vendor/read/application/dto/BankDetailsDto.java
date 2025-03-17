package pl.ecommerce.vendor.read.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bank account details")
public class BankDetailsDto {

	@Schema(description = "Bank account number", example = "PL61109010140000071219812874")
	private String accountNumber;

	@Schema(description = "Name of the bank", example = "International Bank")
	private String bankName;

	@Schema(description = "SWIFT/BIC code of the bank", example = "BREXPLPWXXX")
	private String swiftCode;
}