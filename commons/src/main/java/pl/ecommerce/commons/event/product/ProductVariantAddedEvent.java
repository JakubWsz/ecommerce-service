package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("product.variant.added.event")
public class ProductVariantAddedEvent extends ProductEvent {
	private UUID variantId;
	private String sku;
	private List<ProductAttribute> attributes;
	private BigDecimal price;
	private String currency;
	private int stock;

	@Builder
	public ProductVariantAddedEvent(UUID productId, UUID variantId, String sku,
									List<ProductAttribute> attributes, BigDecimal price,String currency,
									int stock, int version, Instant timestamp,
									@JsonProperty("vendorId") UUID vendorId) {
		super(productId, version, timestamp,vendorId);
		this.variantId = variantId;
		this.sku = sku;
		this.attributes = attributes;
		this.price = price;
		this.currency = currency;
		this.stock = stock;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ProductAttribute {
		private String name;
		private String value;
		private String unit;
	}
}