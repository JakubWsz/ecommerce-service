package pl.ecommerce.commons.model.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
	private UUID id;
	private AddressType addressType;
	private String street;
	private String buildingNumber;
	private String apartmentNumber;
	private String city;
	private String voivodeship;
	private String postalCode;
	private String country;
	private boolean isDefault;
}

