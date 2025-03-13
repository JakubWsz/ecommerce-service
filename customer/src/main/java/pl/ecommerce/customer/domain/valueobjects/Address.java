package pl.ecommerce.customer.domain.valueobjects;

import lombok.Value;

import java.util.UUID;

@Value
public class Address {
	UUID id;
	AddressType addressType;
	String street;
	String buildingNumber;
	String apartmentNumber;
	String city;
	String state;
	String postalCode;
	String country;
	boolean isDefault;
}

