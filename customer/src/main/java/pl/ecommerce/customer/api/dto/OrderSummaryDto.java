package pl.ecommerce.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
	private String id;
	private String customerId;
	private String orderNumber;
	private String status;
	private BigDecimal totalAmount;
	private String currency;
	private int itemCount;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}