package pl.ecommerce.customer.domain.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {
	@Field("street")
	private String street;
	@Field("buildingNumber")
	private String buildingNumber;
	@Field("apartmentNumber")
	private String apartmentNumber;
	@Field("city")
	private String city;
	@Field("state")
	private String state;
	@Field("postalCode")
	private String postalCode;
	@Field("country")
	private String country;
	@Field("isDefault")
	private boolean isDefault;
	@Field("addressType")
	private AddressType addressType;

	public Address( String street, String buildingNumber, String apartmentNumber, String city,
				   String state, String postalCode, String country, boolean isDefault, AddressType addressType) {

		this.street = street;
		this.buildingNumber = buildingNumber;
		this.apartmentNumber = apartmentNumber;
		this.city = city;
		this.state = state;
		this.postalCode = postalCode;
		this.country = country;
		this.isDefault = isDefault;
		this.addressType = addressType;
	}

	public Address(Address other) {
		this.street = other.street;
		this.buildingNumber = other.buildingNumber;
		this.apartmentNumber = other.apartmentNumber;
		this.city = other.city;
		this.state = other.state;
		this.postalCode = other.postalCode;
		this.country = other.country;
		this.isDefault = other.isDefault;
		this.addressType = other.addressType;
	}
}
