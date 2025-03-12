package pl.ecommerce.commons.event.vendor;

import lombok.*;
import org.javamoney.moneta.Money;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.payment.processed.event")
@NoArgsConstructor
public class VendorPaymentProcessedEvent extends DomainEvent {
	private UUID vendorId;
	private UUID paymentId;
	private BigDecimal price;
	private String currencyUnit;
	private LocalDateTime paymentDate;

	@Builder
	public VendorPaymentProcessedEvent(UUID correlationId, UUID vendorId, UUID paymentId,
									   BigDecimal price,String currencyUnit, LocalDateTime paymentDate) {
		super(correlationId);
		this.vendorId = vendorId;
		this.paymentId = paymentId;
		this.price = price;
		this.currencyUnit = currencyUnit;
		this.paymentDate = paymentDate;
	}
}