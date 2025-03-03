package pl.ecommerce.customer.infrastructure.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class GeoLocationResponse {
	private String country;
	private String city;
	private String state;
	private String postalCode;
	private Map<String, String> currency;

}