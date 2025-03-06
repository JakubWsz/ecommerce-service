package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.payment.processed.event")
public class VendorPaymentProcessedEvent extends DomainEvent {
	private UUID vendorId;
	private UUID paymentId;
	private MonetaryAmount amount;
	private LocalDateTime paymentDate;

	@Builder
	public VendorPaymentProcessedEvent(UUID correlationId, UUID vendorId, UUID paymentId,
									   MonetaryAmount amount, LocalDateTime paymentDate) {
		super(correlationId);
		this.vendorId = vendorId;
		this.paymentId = paymentId;
		this.amount = amount;
		this.paymentDate = paymentDate;
	}
}