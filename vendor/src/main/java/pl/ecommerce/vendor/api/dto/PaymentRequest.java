package pl.ecommerce.vendor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;

import javax.money.MonetaryAmount;
import java.util.UUID;

@Builder
public record PaymentRequest(
		@NotNull(message = "Payment Id is required")
		UUID paymentId,
		@NotNull(message = "Vendor Id is required")
		UUID vendorId,
		@NotNull(message = "Amount is required")
		MonetaryAmount amount,
		@NotBlank(message = "Payment method is required")
		String paymentMethod,
		@NotBlank(message = "Bank Account Details is required")
		String bankAccountDetails
) {

	public static PaymentRequest createPaymentRequest(Vendor vendor, VendorPayment payment) {
		return PaymentRequest.builder()
				.vendorId(vendor.getId())
				.paymentId(payment.getId())
				.amount(payment.getAmount())
				.paymentMethod(payment.getPaymentMethod())
				.bankAccountDetails(vendor.getBankAccountDetails())
				.build();
	}
}
