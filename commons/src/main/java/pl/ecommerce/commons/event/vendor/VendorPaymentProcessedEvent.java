package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.payment.processed.event")
@NoArgsConstructor
public class VendorPaymentProcessedEvent extends VendorEvent {
	private UUID vendorId;
	private UUID paymentId;
	private BigDecimal price;
	private String currencyUnit;
	private LocalDateTime paymentDate;

	@Builder
	public VendorPaymentProcessedEvent(UUID vendorId, UUID paymentId,
									   BigDecimal price, String currencyUnit, LocalDateTime paymentDate,
									   int version, Instant timestamp) {
		super(vendorId, version, timestamp);
		this.paymentId = paymentId;
		this.price = price;
		this.currencyUnit = currencyUnit;
		this.paymentDate = paymentDate;
	}
}