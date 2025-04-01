package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("product.stock.updated.event")
public class ProductStockUpdatedEvent extends ProductEvent {
	private int quantity;
	private String warehouseId;

	@Builder
	public ProductStockUpdatedEvent(@JsonProperty("productId") UUID productId,
									@JsonProperty("quantity") int quantity,
									@JsonProperty("warehouseId") String warehouseId,
									@JsonProperty("version") int version,
									@JsonProperty("timestamp") Instant timestamp,
									@JsonProperty("vendorId") UUID vendorId) {
		super(productId, version, timestamp, vendorId);
		this.quantity = quantity;
		this.warehouseId = warehouseId;
	}
}
