package pl.ecommerce.customer.domain.valueobjects;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GeoLocation {
	String country;
	String countryCode;
	String region;
	String city;
	String postalCode;
	Double latitude;
	Double longitude;
	String timezone;
}
