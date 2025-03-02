package pl.ecommerce.customer.domain.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeoLocationData {
	@Field("country")
	private String country;
	@Field("city")
	private String city;
	@Field("voivodeship")
	private String voivodeship;
	@Field("postalCode")
	private String postalCode;

	public GeoLocationData(String country, String city, String voivodeship, String postalCode) {
		this.country = country;
		this.city = city;
		this.voivodeship = voivodeship;
		this.postalCode = postalCode;
	}

	public GeoLocationData(GeoLocationData other) {
		this.country = other.country;
		this.city = other.city;
		this.voivodeship = other.voivodeship;
		this.postalCode = other.postalCode;
	}
}