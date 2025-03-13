package pl.ecommerce.customer.aplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
	private UUID id;
	private String orderNumber;
	private String status;
	private double totalAmount;
	private String currency;
	private int itemCount;
	private Instant createdAt;
	private Instant updatedAt;
}