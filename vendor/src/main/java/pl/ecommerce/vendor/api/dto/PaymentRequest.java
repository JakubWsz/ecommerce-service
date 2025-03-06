package pl.ecommerce.vendor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

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
}
