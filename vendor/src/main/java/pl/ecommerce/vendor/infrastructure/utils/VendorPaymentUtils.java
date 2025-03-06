package pl.ecommerce.vendor.infrastructure.utils;

import pl.ecommerce.vendor.api.dto.PaymentRequest;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

public final class VendorPaymentUtils {

	private VendorPaymentUtils() {
	}

	public static PaymentRequest createPaymentRequest(Vendor vendor, VendorPayment payment) {
		return PaymentRequest.builder()
				.vendorId(vendor.getId())
				.paymentId(payment.getId())
				.amount(payment.getAmount())
				.paymentMethod(payment.getPaymentMethod())
				.bankAccountDetails(vendor.getBankAccountDetails())
				.build();
	}

	public static VendorPayment createVendorPayment(UUID vendorId, MonetaryAmount amount, String paymentMethod) {
		LocalDateTime now = LocalDateTime.now();

		return VendorPayment.builder()
				.id(UUID.randomUUID())
				.vendorId(vendorId)
				.amount(amount)
				.status(VendorPayment.VendorPaymentStatus.PENDING)
				.paymentMethod(paymentMethod)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}
}
