package pl.ecommerce.vendor.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import pl.ecommerce.commons.event.vendor.*;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.VendorService;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorAlreadyExistsException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import pl.ecommerce.vendor.integration.helper.KafkaTopics;
import pl.ecommerce.vendor.integration.helper.TestEventListener;
import pl.ecommerce.vendor.integration.helper.TestUtils;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VendorServiceIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private TestEventListener testEventListener;

	private VendorService vendorService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final String VENDOR_EMAIL = "test.vendor@example.com";
	private Vendor testVendor;

//	@BeforeAll
//	static void setupClass() {
//		setupKafkaConsumer(KafkaTopics.VENDOR_TOPICS);
//		waitForKafkaReady(KafkaTopics.VENDOR_TOPICS);
//	}

	@BeforeEach
	void setupBeforeEach() {
		TestUtils.cleanRepositories(vendorRepository, null, null, null);

		testVendor = TestUtils.createTestVendor(VENDOR_ID, VENDOR_EMAIL);
		vendorRepository.save(testVendor).block();

		vendorService = new VendorService(vendorRepository, eventPublisher);
	}

	@Test
	void registerVendor_WithValidData_ShouldRegisterSuccessfully() {
		Vendor newVendor = Vendor.builder()
				.email("new.vendor@example.com")
				.name("New Vendor")
				.businessName("New Business")
				.gdprConsent(true)
				.build();

		Vendor vendor = vendorService.registerVendor(newVendor).block();

		assertThat(vendor).isNotNull();
		assertThat(vendor.getId()).isNotNull();
		assertThat(vendor.getEmail()).isEqualTo("new.vendor@example.com");
		assertThat(vendor.getName()).isEqualTo("New Vendor");
		assertTrue(vendor.getActive());
		assertThat(vendor.getVendorStatus()).isEqualTo(Vendor.VendorStatus.PENDING);

		var vendorEvents = waitForEvents(VendorRegisteredEvent.class,1000);
		var vendorEvent = vendorEvents.getFirst();

		assertThat(vendorEvents.size()).isEqualTo(1);
		assertEquals(vendor.getEmail(), vendorEvent.getEmail());
		assertEquals(vendor.getVendorStatus().name(), vendorEvent.getStatus());
		assertEquals(vendor.getName(), vendorEvent.getName());

		testEventListener.clearEvents();
	}

	@Test
	void registerVendor_WithoutGdprConsent_ShouldFail() {
		Vendor invalidVendor = Vendor.builder()
				.email("nogdpr@example.com")
				.name("No GDPR Vendor")
				.gdprConsent(false)
				.build();

		Exception exception = null;
		try {
			vendorService.registerVendor(invalidVendor).block();
		} catch (Exception e) {
			exception = e;
		}

		assertNotNull(exception);
		assertInstanceOf(ValidationException.class, exception);
		assertTrue(exception.getMessage().contains("GDPR consent is required"));
	}

	@Test
	void registerVendor_WithExistingEmail_ShouldFail() {
		Vendor duplicateVendor = Vendor.builder()
				.email(VENDOR_EMAIL)
				.name("Duplicate Vendor")
				.gdprConsent(true)
				.build();

		StepVerifier.create(vendorService.registerVendor(duplicateVendor))
				.expectErrorMatches(e -> e instanceof VendorAlreadyExistsException)
				.verify();
	}

	@Test
	void getVendorById_ExistingVendor_ShouldReturnVendor() {
		StepVerifier.create(vendorService.getVendorById(VENDOR_ID))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getEmail()).isEqualTo(VENDOR_EMAIL);
				})
				.verifyComplete();
	}

	@Test
	void getVendorById_NonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(vendorService.getVendorById(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	void getAllVendors_ShouldReturnAllActiveVendors() {
		Vendor secondVendor = Vendor.builder()
				.email("second@example.com")
				.name("Second Vendor")
				.active(true)
				.build();
		vendorRepository.save(secondVendor).block();

		StepVerifier.create(vendorService.getAllVendors().collectList())
				.assertNext(vendors -> {
					assertThat(vendors).hasSize(2);
					assertThat(vendors.stream().map(Vendor::getEmail))
							.contains(VENDOR_EMAIL, "second@example.com");
				})
				.verifyComplete();
	}

	@Test
	void updateVendor_ExistingVendor_ShouldUpdateSuccessfully() {
		Vendor vendorUpdate = Vendor.builder()
				.name("Updated Name")
				.description("Updated Description")
				.phone("987654321")
				.build();

		StepVerifier.create(vendorService.updateVendor(VENDOR_ID, vendorUpdate))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getName()).isEqualTo("Updated Name");
					assertThat(vendor.getDescription()).isEqualTo("Updated Description");
					assertThat(vendor.getPhone()).isEqualTo("987654321");
					assertThat(vendor.getEmail()).isEqualTo(VENDOR_EMAIL);
				})
				.verifyComplete();

		var vendorEvents = waitForEvents(VendorUpdatedEvent.class,1000);
		var vendorEvent = vendorEvents.getFirst();

		assertThat(vendorEvents.size()).isEqualTo(1);
		assertEquals(VENDOR_ID, vendorEvent.getVendorId());
		assertEquals("Updated Name", vendorEvent.getChanges().get("name"));
		assertEquals("Updated Description", vendorEvent.getChanges().get("description"));
		assertEquals("987654321", vendorEvent.getChanges().get("phone"));

		testEventListener.clearEvents();
	}

	@Test
	void updateVendor_WithBusinessAddress_ShouldUpdateSuccessfully() {
		Address newAddress = Address.builder()
				.street("New Street")
				.buildingNumber("5")
				.city("New City")
				.state("New State")
				.postalCode("54321")
				.country("New Country")
				.build();

		Vendor vendorUpdate = Vendor.builder()
				.businessAddress(newAddress)
				.build();

		var vendor =vendorService.updateVendor(VENDOR_ID, vendorUpdate).block();


					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getBusinessAddress()).isNotNull();
					assertThat(vendor.getBusinessAddress().getStreet()).isEqualTo("New Street");
					assertThat(vendor.getBusinessAddress().getCity()).isEqualTo("New City");

		var vendorEvents = waitForEvents(VendorUpdatedEvent.class,1000);
		var vendorEvent = vendorEvents.getFirst();

		assertThat(vendorEvents.size()).isEqualTo(1);
		assertEquals(VENDOR_ID, vendorEvent.getVendorId());
		@SuppressWarnings("unchecked")
		Map<String, Object> businessAddress = (Map<String, Object>) vendorEvent.getChanges().get("businessAddress");

		assertEquals("New Street", businessAddress.get("street"));
		assertEquals("5", businessAddress.get("buildingNumber"));
		assertEquals("54321", businessAddress.get("postalCode"));

		testEventListener.clearEvents();
	}

	@Test
	void updateVendor_NonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();
		Vendor vendorUpdate = Vendor.builder()
				.name("Updated Name")
				.build();

		StepVerifier.create(vendorService.updateVendor(nonExistingId, vendorUpdate))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}

	@Test
	void updateVendorStatus_ExistingVendor_ShouldUpdateStatusSuccessfully() {
	var vendor =	vendorService.updateVendorStatus(VENDOR_ID, Vendor.VendorStatus.SUSPENDED, "Violation").block();

					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getVendorStatus()).isEqualTo(Vendor.VendorStatus.SUSPENDED);
					assertThat(vendor.getStatusChangeReason()).isEqualTo("Violation");

		var vendorEvents = waitForEvents(VendorStatusChangedEvent.class,1000);
		var vendorEvent = vendorEvents.getFirst();

		assertThat(vendorEvents.size()).isEqualTo(1);
		assertEquals(VENDOR_ID, vendorEvent.getVendorId());

		testEventListener.clearEvents();
	}

	@Test
	void updateVerificationStatus_ExistingVendor_ShouldUpdateVerificationSuccessfully() {
		StepVerifier.create(vendorService.updateVerificationStatus(VENDOR_ID, Vendor.VerificationStatus.VERIFIED))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getVerificationStatus()).isEqualTo(Vendor.VerificationStatus.VERIFIED);
				})
				.verifyComplete();

		var vendorEvents = waitForEvents(VendorVerificationCompletedEvent.class,1000);
		var vendorEvent = vendorEvents.getFirst();

		assertThat(vendorEvents.size()).isEqualTo(1);
		assertEquals(VENDOR_ID, vendorEvent.getVendorId());
		assertEquals(Vendor.VerificationStatus.VERIFIED.name(), vendorEvent.getVerificationStatus());
		assertNotNull(vendorEvent.getVerificationTimestamp());

		testEventListener.clearEvents();
	}

	@Test
	void deactivateVendor_ExistingVendor_ShouldDeactivateSuccessfully() {
		StepVerifier.create(vendorService.deactivateVendor(VENDOR_ID)
						.then(vendorRepository.findById(VENDOR_ID)))
				.assertNext(vendor -> {
					assertThat(vendor).isNotNull();
					assertThat(vendor.getId()).isEqualTo(VENDOR_ID);
					assertThat(vendor.getActive()).isFalse();
				})
				.verifyComplete();
	}

	@Test
	void deactivateVendor_NonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();

		StepVerifier.create(vendorService.deactivateVendor(nonExistingId))
				.expectErrorMatches(throwable -> throwable instanceof VendorNotFoundException)
				.verify();
	}
}