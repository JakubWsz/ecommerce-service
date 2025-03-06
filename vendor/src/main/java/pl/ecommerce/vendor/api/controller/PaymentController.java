package pl.ecommerce.vendor.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.vendor.api.mapper.PaymentMapper;
import pl.ecommerce.vendor.api.dto.PaymentRequest;
import pl.ecommerce.vendor.api.dto.PaymentResponse;
import pl.ecommerce.vendor.domain.service.PaymentService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Vendor Payments", description = "Endpoints for managing vendor payments")
@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

	private final PaymentService paymentService;

	@Operation(summary = "Create payment", description = "Creates a new payment for a vendor")
	@PostMapping("/{vendorId}")
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<PaymentResponse> createPayment(
			@PathVariable String vendorId,
			@RequestBody PaymentRequest request) {
		return paymentService.createPayment(
						UUID.fromString(vendorId),
						request.amount(),
						request.paymentMethod())
				.map(PaymentMapper::toResponse);
	}

	@Operation(summary = "Process payment", description = "Processes a pending payment")
	@PostMapping("/{paymentId}/process")
	public Mono<PaymentResponse> processPayment(
			@PathVariable String paymentId) {
		return paymentService.processPayment(UUID.fromString(paymentId))
				.map(PaymentMapper::toResponse);
	}

	@Operation(summary = "Get payment", description = "Gets a payment by ID")
	@GetMapping("/{paymentId}")
	public Mono<PaymentResponse> getPayment(
			@PathVariable String paymentId) {
		return paymentService.getPayment(UUID.fromString(paymentId))
				.map(PaymentMapper::toResponse);
	}

	@Operation(summary = "Get vendor payments", description = "Gets all payments for a vendor")
	@GetMapping("/{vendorId}/payments")
	public Flux<PaymentResponse> getVendorPayments(@PathVariable String vendorId) {
		return paymentService.getVendorPayments(UUID.fromString(vendorId))
				.map(PaymentMapper::toResponse);
	}

	@Operation(summary = "Get payments by status", description = "Gets vendor payments by status")
	@GetMapping("/{vendorId}/payments/status/{status}")
	public Flux<PaymentResponse> getVendorPaymentsByStatus(
			@PathVariable String vendorId,
			@PathVariable String status) {
		return paymentService.getVendorPaymentsByStatus(UUID.fromString(vendorId), status)
				.map(PaymentMapper::toResponse);
	}

	@Operation(summary = "Get payments by date range", description = "Gets vendor payments by date range")
	@GetMapping("/{vendorId}/payments/date-range")
	public Flux<PaymentResponse> getVendorPaymentsByDateRange(
			@PathVariable String  vendorId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
		return paymentService.getVendorPaymentsByDateRange(UUID.fromString(vendorId), startDate, endDate)
				.map(PaymentMapper::toResponse);
	}
}
