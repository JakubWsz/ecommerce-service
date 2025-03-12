package pl.ecommerce.vendor.domain.service;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ecommerce.commons.event.vendor.VendorPaymentProcessedEvent;
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
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

	@Mock
	private VendorPaymentRepository paymentRepository;

	@Mock
	private VendorRepository vendorRepository;

	@Mock
	private PaymentClient paymentClient;

	@Mock
	private EventPublisher eventPublisher;

	@Captor
	private ArgumentCaptor<VendorPaymentProcessedEvent> paymentProcessedEventCaptor;

	@InjectMocks
	private PaymentService paymentService;

	private Vendor vendor;
	private VendorPayment payment;
	private UUID vendorId;
	private UUID paymentId;
	private Money amount;
	private String paymentMethod;
	private LocalDateTime now;

	@BeforeEach
	public void setup() {
		vendorId = UUID.randomUUID();
		paymentId = UUID.randomUUID();
		now = LocalDateTime.now();
		paymentMethod = "BANK_TRANSFER";

		// Initialize monetary amount
		amount = Money.of(100.00,"USD");

		vendor = Vendor.builder()
				.id(vendorId)
				.email("vendor@example.com")
				.name("Test Vendor")
				.active(true)
				.vendorStatus(Vendor.VendorStatus.ACTIVE)
				.verificationStatus(Vendor.VerificationStatus.VERIFIED)
				.build();

		payment = VendorPayment.builder()
				.id(paymentId)
				.vendorId(vendorId)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(paymentMethod)
				.status(VendorPayment.VendorPaymentStatus.PENDING)
				.createdAt(now)
				.build();
	}

	@Test
	public void testCreatePayment_VendorNotFound() {
		when(vendorRepository.findById(any(UUID.class))).thenReturn(Mono.empty());

		StepVerifier.create(paymentService.createPayment(vendorId, amount, paymentMethod))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(paymentRepository, never()).save(any(VendorPayment.class));
	}

	@Test
	public void testCreatePayment_InactiveVendor() {
		vendor.setActive(false);
		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));

		StepVerifier.create(paymentService.createPayment(vendorId, amount, paymentMethod))
				.expectError(PaymentProcessingException.class)
				.verify();

		verify(paymentRepository, never()).save(any(VendorPayment.class));
	}

	@Test
	public void testCreatePayment_Success() {
		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));
		when(paymentRepository.save(any(VendorPayment.class))).thenReturn(Mono.just(payment));

		StepVerifier.create(paymentService.createPayment(vendorId, amount, paymentMethod))
				.expectNextMatches(p ->
						p.getId().equals(paymentId) &&
								p.getVendorId().equals(vendorId) &&
								p.getAmount().equals(amount) &&
								p.getPaymentMethod().equals(paymentMethod) &&
								p.getStatus() == VendorPayment.VendorPaymentStatus.PENDING)
				.verifyComplete();

		verify(paymentRepository).save(any(VendorPayment.class));
	}

	@Test
	public void testProcessPayment_PaymentNotFound() {
		when(paymentRepository.findById(any(UUID.class))).thenReturn(Mono.empty());

		StepVerifier.create(paymentService.processPayment(paymentId))
				.expectError(PaymentProcessingException.class)
				.verify();

		verify(paymentClient, never()).processPayment(any(PaymentRequest.class));
	}

	@Test
	public void testProcessPayment_AlreadyProcessed() {
		payment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);

		when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(payment));

		StepVerifier.create(paymentService.processPayment(paymentId))
				.expectError(PaymentProcessingException.class)
				.verify();

		verify(paymentClient, never()).processPayment(any(PaymentRequest.class));
	}

	@Test
	public void testProcessPayment_VendorNotFound() {
		when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(payment));
		when(vendorRepository.findById(vendorId)).thenReturn(Mono.empty());

		StepVerifier.create(paymentService.processPayment(paymentId))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(paymentClient, never()).processPayment(any(PaymentRequest.class));
	}

	@Test
	public void testProcessPayment_Success() {
		PaymentResponse successResponse = PaymentResponse.builder()
				.id(paymentId)
				.vendorId(vendorId)
				.amount(amount)
				.status(VendorPayment.VendorPaymentStatus.PROCESSED.toString())
				.paymentMethod(paymentMethod)
				.referenceId(UUID.randomUUID())
				.paymentDate(now)
				.createdAt(now.minusMinutes(5))
				.updatedAt(now)
				.build();

		VendorPayment processedPayment = VendorPayment.builder()
				.id(paymentId)
				.vendorId(vendorId)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(paymentMethod)
				.status(VendorPayment.VendorPaymentStatus.PROCESSED)
				.referenceId(successResponse.referenceId())
				.paymentDate(successResponse.paymentDate())
				.createdAt(now)
				.build();

		when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(payment));
		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));
		when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(successResponse));
		when(paymentRepository.save(any(VendorPayment.class))).thenReturn(Mono.just(processedPayment));
		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(paymentService.processPayment(paymentId))
				.expectNextMatches(p ->
						p.getStatus() == VendorPayment.VendorPaymentStatus.PROCESSED &&
								p.getReferenceId().equals(successResponse.referenceId()) &&
								p.getPaymentDate().equals(successResponse.paymentDate()))
				.verifyComplete();

		verify(paymentClient).processPayment(any(PaymentRequest.class));
		verify(paymentRepository).save(any(VendorPayment.class));
		verify(eventPublisher).publish(paymentProcessedEventCaptor.capture());

		VendorPaymentProcessedEvent event = paymentProcessedEventCaptor.getValue();
		assertEquals(vendorId, event.getVendorId());
		assertEquals(paymentId, event.getPaymentId());
		assertEquals(amount.getNumberStripped(), event.getPrice());
		assertEquals(amount.getCurrency().getCurrencyCode(), event.getCurrencyUnit());
	}

	@Test
	public void testProcessPayment_Failed() {
		PaymentResponse failedResponse = PaymentResponse.builder()
				.id(paymentId)
				.vendorId(vendorId)
				.amount(amount)
				.status(VendorPayment.VendorPaymentStatus.FAILED.toString())
				.paymentMethod(paymentMethod)
				.notes("Insufficient funds")
				.createdAt(now)
				.updatedAt(now)
				.build();

		VendorPayment failedPayment = VendorPayment.builder()
				.id(paymentId)
				.vendorId(vendorId)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(paymentMethod)
				.status(VendorPayment.VendorPaymentStatus.FAILED)
				.notes("Insufficient funds")
				.createdAt(now)
				.build();

		when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(payment));
		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));
		when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(failedResponse));
		when(paymentRepository.save(any(VendorPayment.class))).thenReturn(Mono.just(failedPayment));

		StepVerifier.create(paymentService.processPayment(paymentId))
				.expectNextMatches(p ->
						p.getStatus() == VendorPayment.VendorPaymentStatus.FAILED &&
								p.getNotes().equals("Insufficient funds"))
				.verifyComplete();

		verify(paymentClient).processPayment(any(PaymentRequest.class));
		verify(paymentRepository).save(any(VendorPayment.class));
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testGetPayment_Found() {
		when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(payment));

		StepVerifier.create(paymentService.getPayment(paymentId))
				.expectNextMatches(p -> p.getId().equals(paymentId))
				.verifyComplete();
	}

	@Test
	public void testGetPayment_NotFound() {
		when(paymentRepository.findById(any(UUID.class))).thenReturn(Mono.empty());

		StepVerifier.create(paymentService.getPayment(UUID.randomUUID()))
				.expectError(PaymentProcessingException.class)
				.verify();
	}

	@Test
	public void testGetPayments_VendorNotFound() {
		when(vendorRepository.findById(any(UUID.class))).thenReturn(Mono.empty());

		StepVerifier.create(paymentService.getPayments(UUID.randomUUID()))
				.expectError(VendorNotFoundException.class)
				.verify();
	}

	@Test
	public void testGetPayments_Success() {
		VendorPayment payment2 = VendorPayment.builder()
				.id(UUID.randomUUID())
				.vendorId(vendorId)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod("CREDIT_CARD")
				.status(VendorPayment.VendorPaymentStatus.PROCESSED)
				.createdAt(now.minusDays(1))
				.build();

		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));
		when(paymentRepository.findByVendorId(vendorId)).thenReturn(Flux.just(payment, payment2));

		StepVerifier.create(paymentService.getPayments(vendorId))
				.expectNextCount(2)
				.verifyComplete();
	}

	@Test
	public void testGetPaymentsByStatus_VendorNotFound() {
		when(vendorRepository.findById(any(UUID.class))).thenReturn(Mono.empty());

		StepVerifier.create(paymentService.getPaymentsByStatus(
						UUID.randomUUID(), VendorPayment.VendorPaymentStatus.PENDING))
				.expectError(VendorNotFoundException.class)
				.verify();
	}

	@Test
	public void testGetPaymentsByStatus_Success() {
		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));
		when(paymentRepository.findByVendorIdAndStatus(
				eq(vendorId), eq(VendorPayment.VendorPaymentStatus.PENDING)))
				.thenReturn(Flux.just(payment));

		StepVerifier.create(paymentService.getPaymentsByStatus(vendorId, VendorPayment.VendorPaymentStatus.PENDING))
				.expectNextMatches(p ->
						p.getId().equals(paymentId) &&
								p.getStatus() == VendorPayment.VendorPaymentStatus.PENDING)
				.verifyComplete();
	}

	@Test
	public void testGetPaymentsByDateRange_VendorNotFound() {
		LocalDateTime start = now.minusDays(7);
		LocalDateTime end = now;

		when(vendorRepository.findById(any(UUID.class))).thenReturn(Mono.empty());

		StepVerifier.create(paymentService.getPaymentsByDateRange(UUID.randomUUID(), start, end))
				.expectError(VendorNotFoundException.class)
				.verify();
	}

	@Test
	public void testGetPaymentsByDateRange_Success() {
		LocalDateTime start = now.minusDays(7);
		LocalDateTime end = now.plusDays(1);

		payment.setPaymentDate(now);

		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));
		when(paymentRepository.findByVendorIdAndPaymentDateBetween(eq(vendorId), eq(start), eq(end)))
				.thenReturn(Flux.just(payment));

		StepVerifier.create(paymentService.getPaymentsByDateRange(vendorId, start, end))
				.expectNextMatches(p ->
						p.getId().equals(paymentId) &&
								p.getPaymentDate().equals(now))
				.verifyComplete();
	}
}