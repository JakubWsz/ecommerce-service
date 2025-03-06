package pl.ecommerce.vendor.domain.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

@Builder
@Getter
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

	public String getFormattedAddress() {
		return Stream.of(
						nonNull(street) &&
								!street.isEmpty() ? street + (nonNull(buildingNumber) &&
								!buildingNumber.isEmpty() ? " " + buildingNumber : "") : null,
						nonNull(apartmentNumber)
								&& !apartmentNumber.isEmpty() ? "/" + apartmentNumber : null,
						city, state, postalCode, country)
				.filter(Objects::nonNull)
				.collect(Collectors.joining(", "));
	}
	
	public boolean isValid() {
		return nonNull(street) && !street.isEmpty() &&
				nonNull(buildingNumber) && !buildingNumber.isEmpty() &&
				nonNull(city) && !city.isEmpty() &&
				nonNull(country) && !country.isEmpty();
	}
}
