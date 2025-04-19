package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("product.created.event")
@NoArgsConstructor
public class ProductCreatedEvent extends ProductEvent {

	private String productName;
	private String description;
	private BigDecimal price;
	private String currencyUnit;
	private List<UUID> categories;
	private int stockQuantity;
	private Map<String, String> attributes;
	private boolean active;
	private LocalDateTime createdAt;
	private List<String> images;

	@JsonCreator
	@Builder
	public ProductCreatedEvent(
			@JsonProperty("productId") UUID productId,
			@JsonProperty("vendorId") UUID vendorId,
			@JsonProperty("productName") String productName,
			@JsonProperty("description") String description,
			@JsonProperty("price") BigDecimal price,
			@JsonProperty("currencyUnit") String currencyUnit,
			@JsonProperty("categories") List<UUID> categories,
			@JsonProperty("stockQuantity") int stockQuantity,
			@JsonProperty("attributes") Map<String, String> attributes,
			@JsonProperty("createdAt") LocalDateTime createdAt,
			@JsonProperty("images") List<String> images,
			int version,
			Instant timestamp) {
		super(productId, version, timestamp,vendorId);
		this.productName = productName;
		this.description = description;
		this.price = price;
		this.currencyUnit = currencyUnit;
		this.categories = categories;
		this.stockQuantity = stockQuantity;
		this.attributes = attributes;
		this.createdAt = createdAt;
		this.images = images;
	}
}