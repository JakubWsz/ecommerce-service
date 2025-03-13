package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.javamoney.moneta.Money;
import pl.ecommerce.commons.event.DomainEvent;
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
@Message("product.updated.event")
@NoArgsConstructor
public class ProductUpdatedEvent extends ProductEvent {

	private String productName;
	private String description;
	private BigDecimal price;
	private String currencyUnit;
	private List<UUID> categories;
	private Integer stockQuantity;
	private Map<String, String> attributes;
	private LocalDateTime updatedAt;
	private List<String> images;
	private Map<String, Object> changedFields;

	@JsonCreator
	@Builder
	public ProductUpdatedEvent(
			@JsonProperty("productId") UUID productId,
			@JsonProperty("vendorId") UUID vendorId,
			@JsonProperty("productName") String productName,
			@JsonProperty("description") String description,
			@JsonProperty("price") BigDecimal price,
			@JsonProperty("currencyUnit") String currencyUnit,
			@JsonProperty("categories") List<UUID> categories,
			@JsonProperty("stockQuantity") Integer stockQuantity,
			@JsonProperty("attributes") Map<String, String> attributes,
			@JsonProperty("updatedAt") LocalDateTime updatedAt,
			@JsonProperty("images") List<String> images,
			@JsonProperty("changedFields") Map<String, Object> changedFields,
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
		this.updatedAt = updatedAt;
		this.images = images;
		this.changedFields = changedFields;
	}
}