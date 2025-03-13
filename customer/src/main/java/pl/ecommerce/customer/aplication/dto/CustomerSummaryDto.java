package pl.ecommerce.customer.aplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.ecommerce.customer.domain.valueobjects.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryDto {
	private UUID id;
	private String firstName;
	private String lastName;
	private String email;
	private CustomerStatus status;
	private Instant createdAt;
	private int addressCount;
}
