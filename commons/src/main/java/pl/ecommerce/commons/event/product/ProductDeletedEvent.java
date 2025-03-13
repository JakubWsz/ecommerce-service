package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("product.deleted.event")
@NoArgsConstructor
public class ProductDeletedEvent extends ProductEvent {
	private String productName;

	@JsonCreator
	@Builder
	public ProductDeletedEvent(
							   @JsonProperty("productId") UUID productId,
							   @JsonProperty("vendorId") UUID vendorId,
							   @JsonProperty("productName") String productName,
							   int version,
							   Instant timestamp) {
		super(productId, version, timestamp,vendorId);
		this.productName = productName;
	}
}
