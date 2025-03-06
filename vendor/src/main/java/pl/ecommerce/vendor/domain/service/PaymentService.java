package pl.ecommerce.vendor.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.api.dto.PaymentRequest;
import pl.ecommerce.vendor.api.dto.PaymentResponse;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;
import pl.ecommerce.vendor.domain.repository.VendorPaymentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.infrastructure.client.PaymentClient;
import pl.ecommerce.vendor.infrastructure.exception.PaymentProcessingException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

import static pl.ecommerce.vendor.infrastructure.VendorEventUtils.createVendorPaymentProcessedEvent;
import static pl.ecommerce.vendor.infrastructure.utils.VendorPaymentUtils.createPaymentRequest;
import static pl.ecommerce.vendor.infrastructure.utils.VendorPaymentUtils.createVendorPayment;
import static pl.ecommerce.vendor.infrastructure.utils.VendorServiceConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

	private final VendorPaymentRepository paymentRepository;
	private final VendorRepository vendorRepository;
	private final PaymentClient paymentClient;
	private final EventPublisher eventPublisher;

	@Transactional
	public Mono<VendorPayment> createPayment(UUID vendorId, MonetaryAmount amount, String paymentMethod) {
		log.info(LOG_PROCESSING_PAYMENT, vendorId);

		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.flatMap(vendor -> validateAndCreatePayment(vendor, amount, paymentMethod));
	}

	@Transactional
	public Mono<VendorPayment> processPayment(UUID paymentId) {
		return paymentRepository.findById(paymentId)
				.switchIfEmpty(throwPaymentProcessingException(paymentId))
				.flatMap(this::validateAndProcessPayment);
	}

	public Mono<VendorPayment> getPayment(UUID paymentId) {
		return paymentRepository.findById(paymentId)
				.switchIfEmpty(throwPaymentProcessingException(paymentId));
	}

	public Flux<VendorPayment> getVendorPayments(UUID vendorId) {
		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(paymentRepository.findByVendorId(vendorId));
	}

	public Flux<VendorPayment> getVendorPaymentsByStatus(UUID vendorId, String status) {
		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(paymentRepository.findByVendorIdAndStatus(vendorId, status));
	}

	public Flux<VendorPayment> getVendorPaymentsByDateRange(UUID vendorId,
															LocalDateTime start,
															LocalDateTime end) {
		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(paymentRepository.findByVendorIdAndPaymentDateBetween(vendorId, start, end));
	}

	private Mono<VendorPayment> validateAndCreatePayment(Vendor vendor, MonetaryAmount amount, String paymentMethod) {
		if (!vendor.isActive()) {
			return throwPaymentProcessingException(CANNOT_PROCESS_PAYMENT);
		}

		VendorPayment payment = createVendorPayment(vendor.getId(), amount, paymentMethod);
		return paymentRepository.save(payment);
	}

	private Mono<VendorPayment> validateAndProcessPayment(VendorPayment payment) {
		if (!VendorPayment.VendorPaymentStatus.PENDING.equals(payment.getStatus())) {
			return throwPaymentProcessingException(PAYMENT_ALREADY_PROCESSED);
		}
		return vendorRepository.findById(payment.getVendorId())
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + payment.getVendorId())))
				.flatMap(vendor -> processVendorPayment(vendor, payment));
	}

	private Mono<VendorPayment> processVendorPayment(Vendor vendor, VendorPayment payment) {
		PaymentRequest request = createPaymentRequest(vendor, payment);

		return paymentClient.processPayment(request)
				.flatMap(response -> handlePaymentResponse(payment, vendor, response));
	}

	private Mono<VendorPayment> handlePaymentResponse(VendorPayment payment, Vendor vendor, PaymentResponse response) {
		if (VendorPayment.VendorPaymentStatus.PROCESSED.toString().equals(response.status())) {
			updatePaymentAsProcessed(payment, response);
			return paymentRepository.save(payment)
					.doOnSuccess(savedPayment -> publishPaymentProcessedEvent(vendor, savedPayment));
		} else {
			updatePaymentAsFailed(payment, response);
			return paymentRepository.save(payment);
		}
	}

	private void updatePaymentAsProcessed(VendorPayment payment, PaymentResponse response) {
		payment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		payment.setReferenceId(response.referenceId());
		payment.setPaymentDate(response.paymentDate());
		payment.setUpdatedAt(LocalDateTime.now());
	}

	private void updatePaymentAsFailed(VendorPayment payment, PaymentResponse response) {
		payment.setStatus(VendorPayment.VendorPaymentStatus.FAILED);
		payment.setNotes(response.notes());
		payment.setUpdatedAt(LocalDateTime.now());
	}

	private void publishPaymentProcessedEvent(Vendor vendor, VendorPayment payment) {
		var event = createVendorPaymentProcessedEvent(vendor, payment);
		eventPublisher.publish(event);
		log.info(LOG_PAYMENT_PROCESSED, vendor.getId(), payment.getAmount());
	}

	private static Mono<VendorPayment> throwPaymentProcessingException(UUID paymentId) {
		return Mono.error(new PaymentProcessingException(PAYMENT_NOT_FOUND + paymentId));
	}

	private static Mono<VendorPayment> throwPaymentProcessingException(String message) {
		return Mono.error(new PaymentProcessingException(message));
	}
}
