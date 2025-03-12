package pl.ecommerce.commons.event.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("product.deleted.event")
@NoArgsConstructor
public class ProductDeletedEvent extends DomainEvent {
	private UUID productId;
	private UUID vendorId;
	private String productName;

	@JsonCreator
	@Builder
	public ProductDeletedEvent(@JsonProperty("correlationId") UUID correlationId,
							   @JsonProperty("productId") UUID productId,
							   @JsonProperty("vendorId") UUID vendorId,
							   @JsonProperty("productName") String productName) {
		super(correlationId);
		this.productId = productId;
		this.vendorId = vendorId;
		this.productName = productName;
	}
}
