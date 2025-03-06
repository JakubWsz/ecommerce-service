package pl.ecommerce.vendor.api.mapper;

import pl.ecommerce.vendor.api.dto.PaymentResponse;
import pl.ecommerce.vendor.domain.model.VendorPayment;

public class PaymentMapper {

	private PaymentMapper() {
	}

	public static PaymentResponse toResponse(VendorPayment payment) {
		return PaymentResponse.builder()
				.id(payment.getId())
				.vendorId(payment.getVendorId())
				.amount(payment.getAmount())
				.status(String.valueOf(payment.getStatus()))
				.paymentMethod(payment.getPaymentMethod())
				.referenceId(payment.getReferenceId())
				.notes(payment.getNotes())
				.paymentDate(payment.getPaymentDate())
				.createdAt(payment.getCreatedAt())
				.updatedAt(payment.getUpdatedAt())
				.build();
	}
}
