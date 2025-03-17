package pl.ecommerce.vendor.write.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response to bank details update")
public class BankDetailsResponse {

	@Schema(description = "Unique identifier of the vendor",
			example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
	private UUID vendorId;

	@Schema(description = "Response message", example = "Bank details updated successfully")
	private String message;
}