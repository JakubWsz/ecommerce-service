package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("product.price.update.event")
public class ProductPriceUpdatedEvent extends ProductEvent {
	private BigDecimal price;
	private BigDecimal discountedPrice;
	private String currency;

	@Builder
	public ProductPriceUpdatedEvent(@JsonProperty("productId") UUID productId,
									@JsonProperty("quantity") BigDecimal price,
									@JsonProperty("warehouseId") BigDecimal discountedPrice,
									@JsonProperty("warehouseId") String currency,
									@JsonProperty("version") int version,
									@JsonProperty("timestamp") Instant timestamp,
									@JsonProperty("vendorId") UUID vendorId) {
		super(productId, version, timestamp, vendorId);
		this.price = price;
		this.discountedPrice = discountedPrice;
		this.currency = currency;
	}
}
