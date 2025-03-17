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
@Schema(description = "Address information")
public class AddressDto {

	@Schema(description = "Street name", example = "Main Street")
	private String street;

	@Schema(description = "Building number", example = "123")
	private String buildingNumber;

	@Schema(description = "City name", example = "New York")
	private String city;

	@Schema(description = "Postal code", example = "10001")
	private String postalCode;

	@Schema(description = "Country", example = "United States")
	private String country;

	@Schema(description = "State or province", example = "NY")
	private String state;
}