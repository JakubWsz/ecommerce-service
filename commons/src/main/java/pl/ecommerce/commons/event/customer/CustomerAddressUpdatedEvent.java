package pl.ecommerce.commons.event.customer;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Message("customer.address-updated.event")
public class CustomerAddressUpdatedEvent extends CustomerEvent {
	private UUID addressId;
	private String buildingNumber;
	private String apartmentNumber;
	private String street;
	private String city;
	private String postalCode;
	private String country;
	private String voivodeship;
	private boolean isDefault;

	@Builder
	public CustomerAddressUpdatedEvent(UUID customerId, UUID addressId, String buildingNumber, String apartmentNumber,
									   String street, String city, String postalCode,
									   String country, String voivodeship, boolean isDefault,
									   Instant timestamp, int version) {
		super(customerId, version, timestamp);
		this.customerId = customerId;
		this.addressId = addressId;
		this.apartmentNumber=apartmentNumber;
		this.buildingNumber=buildingNumber;
		this.street = street;
		this.city = city;
		this.postalCode = postalCode;
		this.country = country;
		this.voivodeship = voivodeship;
		this.isDefault = isDefault;
	}
}