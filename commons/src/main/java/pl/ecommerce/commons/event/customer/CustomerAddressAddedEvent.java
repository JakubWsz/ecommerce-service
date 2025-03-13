package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;
import pl.ecommerce.commons.model.AddressType;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.address-added.event")
public class CustomerAddressAddedEvent extends CustomerEvent {
	private UUID addressId;
	private AddressType addressType;
	private String buildingNumber;
	private String apartmentNumber;
	private String street;
	private String city;
	private String postalCode;
	private String country;
	private String state;
	private boolean isDefault;

	@Builder
	public CustomerAddressAddedEvent(UUID customerId, UUID addressId,
									 AddressType addressType, String buildingNumber, String apartmentNumber, String street, String city,
									 String postalCode, String country, String state,
									 boolean isDefault, Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.addressId = addressId;
		this.addressType = addressType;
		this.buildingNumber = buildingNumber;
		this.apartmentNumber = apartmentNumber;
		this.street = street;
		this.city = city;
		this.postalCode = postalCode;
		this.country = country;
		this.state = state;
		this.isDefault = isDefault;
	}
}
