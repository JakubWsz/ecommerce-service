package pl.ecommerce.customer.read.aplication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import pl.ecommerce.commons.model.customer.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

@Builder
@Data
@Schema(description = "Summary information of a customer")
public class CustomerSummary {

	@Schema(description = "Unique identifier of the customer", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
	private UUID id;

	@Schema(description = "Customer's first name", example = "John")
	private String firstName;

	@Schema(description = "Customer's last name", example = "Doe")
	private String lastName;

	@Schema(description = "Customer's email address", example = "john.doe@example.com")
	private String email;

	@Schema(description = "Customer status", example = "ACTIVE")
	private CustomerStatus status;

	@Schema(description = "Timestamp when the customer was created", example = "2020-01-01T12:00:00Z")
	private Instant createdAt;

	@Schema(description = "Number of addresses associated with the customer", example = "2")
	private int addressCount;

	@Schema(description = "Trace identifier", example = "12345678-1234-1234-1234-1234567890ab")
	private String traceId;
}
