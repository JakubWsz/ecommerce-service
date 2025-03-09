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

import static pl.ecommerce.vendor.api.dto.PaymentRequest.createPaymentRequest;
import static pl.ecommerce.vendor.infrastructure.VendorEventUtils.createVendorPaymentProcessedEvent;
import static pl.ecommerce.vendor.infrastructure.constant.PaymentConstants.*;

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
		log.info(LOG_OPERATION_STARTED, "Payment creation", "vendor", vendorId);

		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.flatMap(vendor -> validateAndCreatePayment(vendor, amount, paymentMethod))
				.doOnSuccess(payment -> log.info(LOG_ENTITY_CREATED, "Payment", payment.getId()))
				.doOnError(e -> log.error(LOG_ERROR, "payment creation", e.getMessage(), e));
	}

	@Transactional
	public Mono<VendorPayment> processPayment(UUID paymentId) {
		log.info(LOG_OPERATION_STARTED, "Payment processing", "payment", paymentId);

		return paymentRepository.findById(paymentId)
				.switchIfEmpty(throwPaymentProcessingException(paymentId))
				.flatMap(this::validateAndProcessPayment)
				.doOnSuccess(payment -> log.info(LOG_OPERATION_COMPLETED, "Payment processing", "payment", paymentId))
				.doOnError(e -> log.error(LOG_ERROR, "payment processing", e.getMessage(), e));
	}

	public Mono<VendorPayment> getPayment(UUID paymentId) {
		log.debug(LOG_OPERATION_STARTED, "Payment retrieval", "payment", paymentId);

		return paymentRepository.findById(paymentId)
				.switchIfEmpty(throwPaymentProcessingException(paymentId))
				.doOnSuccess(payment -> log.debug(LOG_OPERATION_COMPLETED, "Payment retrieval", "payment", paymentId));
	}

	public Flux<VendorPayment> getPayments(UUID vendorId) {
		log.debug(LOG_OPERATION_STARTED, "Payments retrieval", "vendor", vendorId);

		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(paymentRepository.findByVendorId(vendorId))
				.doOnComplete(() -> log.debug(LOG_OPERATION_COMPLETED, "Payments retrieval", "vendor", vendorId));
	}

	public Flux<VendorPayment> getPaymentsByStatus(UUID vendorId, VendorPayment.VendorPaymentStatus status) {
		log.debug(LOG_OPERATION_STARTED, "Payment retrieval by status", "vendor", vendorId);

		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(paymentRepository.findByVendorIdAndStatus(vendorId, status))
				.doOnComplete(() -> log.debug(LOG_OPERATION_COMPLETED, "Payment retrieval by status", "vendor", vendorId));
	}

	public Flux<VendorPayment> getPaymentsByDateRange(UUID vendorId,
													  LocalDateTime start,
													  LocalDateTime end) {
		log.debug(LOG_OPERATION_STARTED, "Payment retrieval by date range", "vendor", vendorId);

		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + vendorId)))
				.thenMany(paymentRepository.findByVendorIdAndPaymentDateBetween(vendorId, start, end))
				.doOnComplete(() -> log.debug(LOG_OPERATION_COMPLETED, "Payment retrieval by date range", "vendor", vendorId));
	}

	private Mono<VendorPayment> validateAndCreatePayment(Vendor vendor, MonetaryAmount amount, String paymentMethod) {
		if (!vendor.getActive()) {
			log.warn("Cannot process payment for inactive vendor: {}", vendor.getId());
			return throwPaymentProcessingException(ERROR_CANNOT_PROCESS_PAYMENT);
		}

		VendorPayment payment = VendorPayment.create(vendor.getId(), amount, paymentMethod);
		return paymentRepository.save(payment);
	}

	private Mono<VendorPayment> validateAndProcessPayment(VendorPayment payment) {
		if (!VendorPayment.VendorPaymentStatus.PENDING.equals(payment.getStatus())) {
			log.warn("Payment already processed: {}", payment.getId());
			return throwPaymentProcessingException(ERROR_PAYMENT_ALREADY_PROCESSED);
		}
		return vendorRepository.findById(payment.getVendorId())
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + payment.getVendorId())))
				.flatMap(vendor -> processVendorPayment(vendor, payment));
	}

	private Mono<VendorPayment> processVendorPayment(Vendor vendor, VendorPayment payment) {
		log.info("Processing payment for vendor: {}, amount: {}", vendor.getId(), payment.getAmount());
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
			log.warn("Payment processing failed: {}, reason: {}", payment.getId(), response.notes());
			updatePaymentAsFailed(payment, response);
			return paymentRepository.save(payment);
		}
	}

	private void updatePaymentAsProcessed(VendorPayment payment, PaymentResponse response) {
		payment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		payment.setReferenceId(response.referenceId());
		payment.setPaymentDate(response.paymentDate());
	}

	private void updatePaymentAsFailed(VendorPayment payment, PaymentResponse response) {
		payment.setStatus(VendorPayment.VendorPaymentStatus.FAILED);
		payment.setNotes(response.notes());
	}

	private void publishPaymentProcessedEvent(Vendor vendor, VendorPayment payment) {
		var event = createVendorPaymentProcessedEvent(vendor, payment);
		eventPublisher.publish(event);
		log.info(LOG_PAYMENT_PROCESSED, vendor.getId(), payment.getAmount());
	}

	private static Mono<VendorPayment> throwPaymentProcessingException(UUID paymentId) {
		return Mono.error(new PaymentProcessingException(ERROR_PAYMENT_NOT_FOUND + paymentId));
	}

	private static Mono<VendorPayment> throwPaymentProcessingException(String message) {
		return Mono.error(new PaymentProcessingException(message));
	}
}
