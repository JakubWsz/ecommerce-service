package pl.ecommerce.vendor.integration;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import pl.ecommerce.commons.event.vendor.VendorPaymentProcessedEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.api.dto.PaymentRequest;
import pl.ecommerce.vendor.api.dto.PaymentResponse;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;
import pl.ecommerce.vendor.domain.repository.VendorPaymentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.PaymentService;
import pl.ecommerce.vendor.infrastructure.client.PaymentClient;
import pl.ecommerce.vendor.infrastructure.exception.PaymentProcessingException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import pl.ecommerce.vendor.integration.helper.TestEventListener;
import pl.ecommerce.vendor.integration.helper.TestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class PaymentServiceIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private VendorPaymentRepository paymentRepository;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private TestEventListener testEventListener;

	@Mock
	private PaymentClient paymentClient;

	private PaymentService paymentService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final UUID PAYMENT_ID = UUID.randomUUID();
	private static final String PAYMENT_METHOD = "BANK_TRANSFER";
	private Vendor testVendor;
	private VendorPayment testPayment;
	private Money amount;

	@BeforeEach
	void setupBeforeEach() {
		TestUtils.cleanRepositories(vendorRepository, paymentRepository, null, null);

		amount = Money.of(100.00, "USD");

		testVendor = TestUtils.createTestVendor(VENDOR_ID, "test.vendor@example.com");

		testPayment = TestUtils.createTestPayment(PAYMENT_ID, VENDOR_ID, amount, PAYMENT_METHOD);

		vendorRepository.save(testVendor).block();
		paymentRepository.save(testPayment).block();

		paymentService = new PaymentService(paymentRepository, vendorRepository, paymentClient, eventPublisher);
	}

	@Test
	@Order(1)
	void createPayment_WithValidData_ShouldCreatePaymentSuccessfully() {
		MonetaryAmount paymentAmount = TestUtils.createMonetaryAmount(250.00, "USD");

		StepVerifier.create(paymentService.createPayment(VENDOR_ID, paymentAmount, "CREDIT_CARD"))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isNotNull();
					assertThat(payment.getVendorId()).isEqualTo(VENDOR_ID);
					assertThat(payment.getAmount()).isEqualTo(paymentAmount);
					assertThat(payment.getPaymentMethod()).isEqualTo("CREDIT_CARD");
					assertThat(payment.getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PENDING);
				})
				.verifyComplete();
	}

	@Test
	@Order(2)
	void createPayment_WithInactiveVendor_ShouldFail() {
		testVendor.setActive(false);
		vendorRepository.save(testVendor).block();

		StepVerifier.create(paymentService.createPayment(VENDOR_ID, amount, PAYMENT_METHOD))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException &&
						throwable.getMessage().contains("Cannot process payment for inactive vendor"))
				.verify();
	}

	@Test
	@Order(3)
	void createPayment_WithNonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(paymentService.createPayment(nonExistingId, amount, PAYMENT_METHOD))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	@Order(4)
	void processPayment_WithSuccessfulResponse_ShouldProcessPaymentSuccessfully() {
		LocalDateTime now = LocalDateTime.now();
		UUID referenceId = UUID.randomUUID();
		PaymentResponse successResponse = PaymentResponse.builder()
				.id(PAYMENT_ID)
				.vendorId(VENDOR_ID)
				.amount(amount)
				.status(VendorPayment.VendorPaymentStatus.PROCESSED.toString())
				.paymentMethod(PAYMENT_METHOD)
				.referenceId(referenceId)
				.paymentDate(now)
				.createdAt(now.minusMinutes(5))
				.updatedAt(now)
				.build();

		when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(successResponse));

		StepVerifier.create(paymentService.processPayment(PAYMENT_ID))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
					assertThat(payment.getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PROCESSED);
					assertThat(payment.getReferenceId()).isEqualTo(successResponse.referenceId());
					assertThat(payment.getPaymentDate()).isEqualTo(successResponse.paymentDate());
				})
				.verifyComplete();

		var vendorEvents = waitForEvents(VendorPaymentProcessedEvent.class,1000);
		var vendorEvent = vendorEvents.getFirst();

		assertThat(vendorEvents.size()).isEqualTo(1);
		assertEquals(VENDOR_ID, vendorEvent.getVendorId());
		assertEquals(PAYMENT_ID, vendorEvent.getPaymentId());
		assertEquals(amount.getCurrency().getCurrencyCode(), vendorEvent.getCurrencyUnit());
		assertEquals(amount.getNumberStripped(), vendorEvent.getPrice());

		testEventListener.clearEvents();
	}

	@Test
	@Order(5)
	void processPayment_WithFailedResponse_ShouldMarkPaymentAsFailed() {
		PaymentResponse failedResponse = PaymentResponse.builder()
				.id(PAYMENT_ID)
				.vendorId(VENDOR_ID)
				.amount(amount)
				.status(VendorPayment.VendorPaymentStatus.FAILED.toString())
				.paymentMethod(PAYMENT_METHOD)
				.notes("Insufficient funds")
				.createdAt(LocalDateTime.now())
				.build();

		when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(Mono.just(failedResponse));

		StepVerifier.create(paymentService.processPayment(PAYMENT_ID))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
					assertThat(payment.getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.FAILED);
					assertThat(payment.getNotes()).isEqualTo("Insufficient funds");
				})
				.verifyComplete();
	}

	@Test
	@Order(6)
	void processPayment_WithAlreadyProcessedPayment_ShouldFail() {
		testPayment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		paymentRepository.save(testPayment).block();

		StepVerifier.create(paymentService.processPayment(PAYMENT_ID))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException &&
						throwable.getMessage().contains("Payment already processed"))
				.verify();
	}

	@Test
	@Order(7)
	void processPayment_WithNonExistingPayment_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(paymentService.processPayment(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException)
				.verify();
	}

	@Test
	@Order(8)
	void getPayment_ExistingPayment_ShouldReturnPayment() {
		StepVerifier.create(paymentService.getPayment(PAYMENT_ID))
				.assertNext(payment -> {
					assertThat(payment).isNotNull();
					assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
					assertThat(payment.getVendorId()).isEqualTo(VENDOR_ID);
				})
				.verifyComplete();
	}

	@Test
	@Order(9)
	void getPayment_NonExistingPayment_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(paymentService.getPayment(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof PaymentProcessingException)
				.verify();
	}

	@Test
	@Order(9)
	void getPayments_ExistingVendor_ShouldReturnAllPayments() {
		VendorPayment secondPayment = TestUtils.createTestPayment(UUID.randomUUID(), VENDOR_ID, amount, PAYMENT_METHOD);
		paymentRepository.save(secondPayment).block();

		StepVerifier.create(paymentService.getPayments(VENDOR_ID).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(2);
					assertThat(payments.stream().map(VendorPayment::getVendorId))
							.containsOnly(VENDOR_ID);
				})
				.verifyComplete();
	}

	@Test
	@Order(10)
	void getPaymentsByStatus_ShouldReturnFilteredPayments() {
		VendorPayment processedPayment = TestUtils.createTestPayment(UUID.randomUUID(), VENDOR_ID, amount, PAYMENT_METHOD);
		processedPayment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		processedPayment.setPaymentDate(LocalDateTime.now());

		paymentRepository.save(processedPayment).block();

		StepVerifier.create(paymentService.getPaymentsByStatus(VENDOR_ID, VendorPayment.VendorPaymentStatus.PENDING).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(1);
					assertThat(payments.getFirst().getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PENDING);
				})
				.verifyComplete();

		StepVerifier.create(paymentService.getPaymentsByStatus(VENDOR_ID, VendorPayment.VendorPaymentStatus.PROCESSED).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(1);
					assertThat(payments.getFirst().getStatus()).isEqualTo(VendorPayment.VendorPaymentStatus.PROCESSED);
				})
				.verifyComplete();
	}

	@Test
	@Order(11)
	void getPaymentsByDateRange_ShouldReturnFilteredPayments() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime yesterday = now.minusDays(1);
		LocalDateTime tomorrow = now.plusDays(1);

		testPayment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		testPayment.setPaymentDate(yesterday);
		paymentRepository.save(testPayment).block();

		VendorPayment futurePayment = TestUtils.createTestPayment(UUID.randomUUID(), VENDOR_ID, amount, PAYMENT_METHOD);
		futurePayment.setStatus(VendorPayment.VendorPaymentStatus.PROCESSED);
		futurePayment.setPaymentDate(tomorrow);
		paymentRepository.save(futurePayment).block();

		StepVerifier.create(paymentService.getPaymentsByDateRange(VENDOR_ID, now, tomorrow.plusDays(1)).collectList())
				.assertNext(payments -> {
					assertThat(payments).hasSize(1);
					assertThat(payments.getFirst().getPaymentDate()).isAfter(now);
				})
				.verifyComplete();

		StepVerifier.create(paymentService.getPaymentsByDateRange(VENDOR_ID, yesterday.minusDays(1), tomorrow.plusDays(1)).collectList())
				.assertNext(payments -> assertThat(payments).hasSize(2))
				.verifyComplete();
	}

}