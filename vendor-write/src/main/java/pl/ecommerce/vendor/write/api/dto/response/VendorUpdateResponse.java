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
@Schema(description = "Response to vendor update")
public class VendorUpdateResponse {

	@Schema(description = "Unique identifier of the updated vendor",
			example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
	private UUID vendorId;

	@Schema(description = "Response message", example = "Vendor updated successfully")
	private String message;
}