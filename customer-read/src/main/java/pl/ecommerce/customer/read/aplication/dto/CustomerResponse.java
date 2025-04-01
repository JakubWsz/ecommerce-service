package pl.ecommerce.customer.read.aplication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@Schema(description = "Detailed customer information")
public class CustomerResponse {

	@Schema(description = "Unique identifier of the customer", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
	private UUID id;

	@Schema(description = "Customer's email address", example = "john.doe@example.com")
	private String email;

	@Schema(description = "Customer's first name", example = "John")
	private String firstName;

	@Schema(description = "Customer's last name", example = "Doe")
	private String lastName;

	@Schema(description = "Customer's phone number", example = "+123456789")
	private String phoneNumber;

	@Schema(description = "Indicates if the customer's email is verified", example = "true")
	private boolean emailVerified;

	@Schema(description = "Customer status", example = "ACTIVE")
	private String status;

	@Schema(description = "Timestamp when the customer was created", example = "2020-01-01T12:00:00Z")
	private Instant createdAt;

	@Schema(description = "Timestamp when the customer was last updated", example = "2020-01-02T12:00:00Z")
	private Instant updatedAt;

	@Schema(description = "List of customer addresses")
	private List<AddressResponse> addresses;

	@Schema(description = "Customer preferences")
	private PreferencesResponse preferences;

	@Schema(description = "Trace identifier", example = "12345678-1234-1234-1234-1234567890ab")
	private String traceId;
}