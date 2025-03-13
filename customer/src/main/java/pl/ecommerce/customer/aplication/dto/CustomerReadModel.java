package pl.ecommerce.customer.aplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.ecommerce.customer.domain.valueobjects.CustomerStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class CustomerReadModel {
	@Id
	private UUID id;
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private boolean emailVerified;
	private boolean phoneVerified;
	private CustomerStatus status;
	private Instant createdAt;
	private Instant updatedAt;
	private String registrationIp;
	private PersonalDataDto personalData;
	private List<AddressDto> addresses;
	private GeoLocationDataDto geoLocationData;
	private CustomerPreferencesDto preferences;
	private Map<String, String> metadata;
}

