package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@Message("ProductUpdatedEvent")
public class ProductUpdatedEvent extends DomainEvent {

	private final UUID productId;
	private final String vendorId;
	private final String productName;
	private final String description;
	private final BigDecimal price;
	private final String currency;
	private final List<UUID> categories;
	private final Integer stockQuantity;
	private final Map<String, String> attributes;
	private final String status;
	private final LocalDateTime updatedAt;
	private final String updatedBy;
	private final List<String> images;
	private final String sku;
	private final Map<String, Object> changedFields;

	@JsonCreator
	public ProductUpdatedEvent(
			@JsonProperty("correlationId") UUID correlationId,
			@JsonProperty("productId") UUID productId,
			@JsonProperty("vendorId") String vendorId,
			@JsonProperty("productName") String productName,
			@JsonProperty("description") String description,
			@JsonProperty("price") BigDecimal price,
			@JsonProperty("currency") String currency,
			@JsonProperty("categories") List<UUID> categories,
			@JsonProperty("stockQuantity") Integer stockQuantity,
			@JsonProperty("attributes") Map<String, String> attributes,
			@JsonProperty("status") String status,
			@JsonProperty("updatedAt") LocalDateTime updatedAt,
			@JsonProperty("updatedBy") String updatedBy,
			@JsonProperty("images") List<String> images,
			@JsonProperty("sku") String sku,
			@JsonProperty("changedFields") Map<String, Object> changedFields) {
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
		this.updatedAt = updatedAt;
		this.updatedBy = updatedBy;
		this.images = images;
		this.sku = sku;
		this.changedFields = changedFields;
	}
}