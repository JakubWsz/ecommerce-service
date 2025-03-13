package pl.ecommerce.customer.aplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.ecommerce.customer.domain.valueobjects.AddressType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
	private UUID id;
	private String street;
	private String buildingNumber;
	private String apartmentNumber;
	private String city;
	private String state;
	private String postalCode;
	private String country;
	private boolean isDefault;
	private AddressType addressType;
}