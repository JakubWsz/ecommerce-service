package pl.ecommerce.vendor.domain.model;

import lombok.*;
import org.javamoney.moneta.Money;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static pl.ecommerce.vendor.domain.model.VendorPayment.VendorPaymentStatus.*;

import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.money.CurrencyUnit;

import org.springframework.data.mongodb.core.mapping.Field;

@ToString
@SuperBuilder
@Getter
@Document(collection = "vendor_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VendorPayment extends BaseEntity {

	@Indexed
	@Field("vendorId")
	private UUID vendorId;

	@Field("amount")
	@Getter(AccessLevel.NONE)
	private BigDecimal amount;

	@Field("currency")
	@Getter(AccessLevel.NONE)
	private String currency;

	@Setter
	@Field("status")
	private VendorPaymentStatus status;

	@Field("paymentMethod")
	private String paymentMethod;

	@Setter
	@Field("referenceId")
	private UUID referenceId;

	@Setter
	@Field("notes")
	private String notes;

	@Setter
	@Field("paymentDate")
	private LocalDateTime paymentDate;

	@Setter
	@Field("statusChangeReason")
	private String statusChangeReason;

	public static VendorPayment create(UUID vendorId, MonetaryAmount amount, String paymentMethod) {
		VendorPayment payment = VendorPayment.builder()
				.vendorId(vendorId)
				.status(PENDING)
				.paymentMethod(paymentMethod)
				.build();
		payment.setAmount(amount);
		return payment;
	}

	public Money getAmount() {
		if (amount == null || currency == null) {
			return null;
		}
		return Money.of(amount, currency);
	}

	private void setAmount(MonetaryAmount monetaryAmount) {
		if (monetaryAmount == null) {
			this.amount = null;
			this.currency = null;
		} else {
			this.amount = monetaryAmount.getNumber().numberValue(BigDecimal.class);
			this.currency = monetaryAmount.getCurrency().getCurrencyCode();
		}
	}

	public boolean isPending() {
		return PENDING.equals(status);
	}

	public boolean isProcessed() {
		return PROCESSED.equals(status);
	}

	public boolean isFailed() {
		return FAILED.equals(status);
	}

	public boolean canTransitionTo(VendorPaymentStatus newStatus) {
		if (this.status == PROCESSED && newStatus != PROCESSED) {
			return false;
		}

		return this.status != FAILED || newStatus != PROCESSED;
	}

	public enum VendorPaymentStatus {
		PENDING, PROCESSED, FAILED, CANCELLED
	}
}