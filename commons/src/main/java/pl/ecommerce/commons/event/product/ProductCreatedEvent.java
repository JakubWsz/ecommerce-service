package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("ProductCreatedEvent")
public class ProductCreatedEvent extends DomainEvent {

	private UUID productId;
	private String vendorId;
	private String productName;
	private String description;
	private BigDecimal price;
	private String currency;
	private List<UUID> categories;
	private int stockQuantity;
	private Map<String, String> attributes;
	private String status;
	private LocalDateTime createdAt;
	private String createdBy;
	private List<String> images;
	private String sku;

	@JsonCreator
	@Builder
	public ProductCreatedEvent(
			@JsonProperty("correlationId") UUID correlationId,
			@JsonProperty("productId") UUID productId,
			@JsonProperty("vendorId") String vendorId,
			@JsonProperty("productName") String productName,
			@JsonProperty("description") String description,
			@JsonProperty("price") BigDecimal price,
			@JsonProperty("currency") String currency,
			@JsonProperty("categories") List<UUID> categories,
			@JsonProperty("stockQuantity") int stockQuantity,
			@JsonProperty("attributes") Map<String, String> attributes,
			@JsonProperty("status") String status,
			@JsonProperty("createdAt") LocalDateTime createdAt,
			@JsonProperty("createdBy") String createdBy,
			@JsonProperty("images") List<String> images,
			@JsonProperty("sku") String sku) {
		super(correlationId);
		this.productId = productId;
		this.vendorId = vendorId;
		this.productName = productName;
		this.description = description;
		this.price = price;
		this.currency = currency;
		this.categories = categories;
		this.stockQuantity = stockQuantity;
		this.attributes = attributes;
		this.status = status;
		this.createdAt = createdAt;
		this.createdBy = createdBy;
		this.images = images;
		this.sku = sku;
	}
}