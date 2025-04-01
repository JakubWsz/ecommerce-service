package pl.ecommerce.customer.read.aplication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Details of an address")
public class AddressResponse {

	@Schema(description = "Street name", example = "Main Street")
	private String street;

	@Schema(description = "Building number", example = "123")
	private String buildingNumber;

	@Schema(description = "Apartment number", example = "45")
	private String apartmentNumber;

	@Schema(description = "City", example = "Warsaw")
	private String city;

	@Schema(description = "Voivodeship", example = "Masovian")
	private String voivodeship;

	@Schema(description = "Postal code", example = "00-001")
	private String postalCode;

	@Schema(description = "Country", example = "USA")
	private String country;

	@Schema(description = "Indicates if this is the default address", example = "true")
	private boolean isDefault;

	@Schema(description = "Type of address", example = "HOME")
	private String addressType;
}