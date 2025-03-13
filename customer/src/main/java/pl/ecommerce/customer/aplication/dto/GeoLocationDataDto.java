package pl.ecommerce.customer.aplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationDataDto {
	private String country;
	private String city;
	private String region;
	private String postalCode;
	private String countryCode;
}

