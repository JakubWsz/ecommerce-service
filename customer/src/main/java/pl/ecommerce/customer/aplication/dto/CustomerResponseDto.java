package pl.ecommerce.customer.aplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDto {
	private UUID id;
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private boolean emailVerified;
	private String status;
	private Instant createdAt;
	private Instant updatedAt;
	private List<AddressDto> addresses;
	private GeoLocationDataDto geoLocationData;
	private CustomerPreferencesDto preferences;
}