package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockVerificationRequest {
	private List<StockVerificationItem> items;
	private String orderId;
	private UUID customerId;
}