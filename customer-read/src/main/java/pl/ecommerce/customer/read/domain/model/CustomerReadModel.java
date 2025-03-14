package pl.ecommerce.customer.read.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.ecommerce.commons.customer.model.Address;
import pl.ecommerce.commons.customer.model.CustomerPreferences;
import pl.ecommerce.commons.customer.model.CustomerStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
	private PersonalData personalData;
	private List<Address> addresses = new ArrayList<>();
	private CustomerPreferences preferences;
	private Map<String, String> metadata = new HashMap<>();
	private String lastTraceId;
	private String lastSpanId;
	private String lastOperation;
	private Instant lastUpdatedAt;
}