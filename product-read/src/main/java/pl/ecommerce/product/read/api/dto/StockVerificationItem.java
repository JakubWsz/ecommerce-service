package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockVerificationItem {
	private UUID productId;
	private int quantity;
	private int availableQuantity;
}