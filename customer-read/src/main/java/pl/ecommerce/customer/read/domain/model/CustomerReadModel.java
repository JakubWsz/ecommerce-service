package pl.ecommerce.customer.read.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pl.ecommerce.commons.model.customer.Address;
import pl.ecommerce.commons.model.customer.CustomerPreferences;
import pl.ecommerce.commons.model.customer.CustomerStatus;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class CustomerReadModel {
	@Id
	@Field("_id")
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