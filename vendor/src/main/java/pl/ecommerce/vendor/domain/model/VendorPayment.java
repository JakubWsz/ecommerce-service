package pl.ecommerce.vendor.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

import static pl.ecommerce.vendor.domain.model.VendorPayment.VendorPaymentStatus.*;

@Builder
@Getter
@Document(collection = "vendor_payments")
public class VendorPayment {

	@Id
	private UUID id;
	private UUID vendorId;
	private MonetaryAmount amount;
	@Setter
	private VendorPaymentStatus status;
	private String paymentMethod;
	@Setter
	private UUID referenceId;
	@Setter
	private String notes;
	@Setter
	private LocalDateTime paymentDate;
	private LocalDateTime createdAt;
	@Setter
	private LocalDateTime updatedAt;

	public boolean isPending() {
		return PENDING.equals(status);
	}

	public boolean isProcessed() {
		return PROCESSED.equals(status);
	}

	public boolean isFailed() {
		return FAILED.equals(status);
	}

	public enum VendorPaymentStatus {
		PENDING, PROCESSED, FAILED
	}
}
