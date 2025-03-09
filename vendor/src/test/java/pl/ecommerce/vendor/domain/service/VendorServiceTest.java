package pl.ecommerce.vendor.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ecommerce.commons.event.vendor.VendorRegisteredEvent;
import pl.ecommerce.commons.event.vendor.VendorStatusChangedEvent;
import pl.ecommerce.commons.event.vendor.VendorUpdatedEvent;
import pl.ecommerce.commons.event.vendor.VendorVerificationCompletedEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.infrastructure.VendorValidator;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorAlreadyExistsException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pl.ecommerce.vendor.infrastructure.VendorValidator.isValidVerificationStatus;

@ExtendWith(MockitoExtension.class)
public class VendorServiceTest {

	@Mock
	private VendorRepository vendorRepository;

	@Mock
	private EventPublisher eventPublisher;

	@Captor
	private ArgumentCaptor<VendorRegisteredEvent> registeredEventCaptor;

	@Captor
	private ArgumentCaptor<VendorUpdatedEvent> updatedEventCaptor;

	@Captor
	private ArgumentCaptor<VendorStatusChangedEvent> statusChangedEventCaptor;

	@Captor
	private ArgumentCaptor<VendorVerificationCompletedEvent> verificationEventCaptor;

	@InjectMocks
	private VendorService vendorService;

	private Vendor vendor;
	private UUID vendorId;

	@BeforeEach
	public void setup() {
		vendorId = UUID.randomUUID();

		Address businessAddress = Address.builder()
				.street("123 Main St")
				.buildingNumber("45")
				.apartmentNumber("B")
				.city("City")
				.state("State")
				.postalCode("12345")
				.country("Country")
				.build();

		vendor = Vendor.builder()
				.id(vendorId)
				.email("vendor@example.com")
				.name("Test Vendor")
				.description("Test Description")
				.phone("123456789")
				.businessName("Test Business")
				.taxId("TAX-12345")
				.businessAddress(businessAddress)
				.bankAccountDetails("Bank Account Details")
				.gdprConsent(true)
				.active(true)
				.vendorStatus(Vendor.VendorStatus.ACTIVE)
				.verificationStatus(Vendor.VerificationStatus.PENDING)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	public void testRegisterVendor_DuplicateEmail_ShouldFail() {
		when(vendorRepository.existsByEmail(anyString())).thenReturn(Mono.just(true));

		StepVerifier.create(vendorService.registerVendor(vendor))
				.expectErrorMatches(e -> e instanceof VendorAlreadyExistsException)
				.verify();

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testRegisterVendor_Success() {
		when(vendorRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
		when(vendorRepository.save(any(Vendor.class))).thenReturn(Mono.just(vendor));
		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(vendorService.registerVendor(vendor))
				.expectNextMatches(v -> v.getEmail().equals("vendor@example.com"))
				.verifyComplete();

		verify(eventPublisher).publish(registeredEventCaptor.capture());
		VendorRegisteredEvent event = registeredEventCaptor.getValue();
		assertEquals(vendorId, event.getVendorId());
		assertEquals("vendor@example.com", event.getEmail());
		assertEquals("Test Vendor", event.getName());
	}

	@Test
	public void testGetVendorById_Found() {
		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(vendor));

		StepVerifier.create(vendorService.getVendorById(vendorId))
				.expectNextMatches(v -> v.getId().equals(vendorId))
				.verifyComplete();

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testGetVendorById_NotFound() {
		UUID nonExistentId = UUID.randomUUID();
		when(vendorRepository.findById(nonExistentId)).thenReturn(Mono.empty());

		StepVerifier.create(vendorService.getVendorById(nonExistentId))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testGetAllVendors_ReturnsAllActiveVendors() {
		Vendor vendor2 = Vendor.builder()
				.id(UUID.randomUUID())
				.email("vendor2@example.com")
				.active(true)
				.build();

		when(vendorRepository.findByActiveTrue()).thenReturn(Flux.just(vendor, vendor2));

		StepVerifier.create(vendorService.getAllVendors())
				.expectNextCount(2)
				.verifyComplete();

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testGetAllVendors_EmptyList() {
		when(vendorRepository.findByActiveTrue()).thenReturn(Flux.empty());

		StepVerifier.create(vendorService.getAllVendors())
				.expectNextCount(0)
				.verifyComplete();

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testUpdateVendor_VendorNotFound() {
		UUID nonExistentId = UUID.randomUUID();
		when(vendorRepository.findById(nonExistentId)).thenReturn(Mono.empty());

		StepVerifier.create(vendorService.updateVendor(nonExistentId, vendor))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorRepository, never()).save(any(Vendor.class));
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testUpdateVendor_Success() {
		Vendor existingVendor = Vendor.builder()
				.id(vendorId)
				.email("old@example.com")
				.name("Old Name")
				.build();

		Address newAddress = Address.builder()
				.street("New Street")
				.buildingNumber("123")
				.city("New City")
				.state("New State")
				.postalCode("54321")
				.country("New Country")
				.build();

		Vendor update = Vendor.builder()
				.id(vendorId)
				.name("New Name")
				.phone("987654321")
				.businessAddress(newAddress)
				.build();

		Vendor updatedVendor = Vendor.builder()
				.id(vendorId)
				.email("old@example.com")
				.name("New Name")
				.phone("987654321")
				.businessAddress(newAddress)
				.build();

		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(existingVendor));
		when(vendorRepository.save(any(Vendor.class))).thenReturn(Mono.just(updatedVendor));
		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(vendorService.updateVendor(vendorId, update))
				.expectNextMatches(v -> v.getName().equals("New Name") &&
						v.getPhone().equals("987654321") &&
						v.getBusinessAddress().getStreet().equals("New Street"))
				.verifyComplete();

		verify(eventPublisher).publish(updatedEventCaptor.capture());
		VendorUpdatedEvent event = updatedEventCaptor.getValue();
		assertEquals(vendorId, event.getVendorId());
		assertNotNull(event.getChanges());
		assertTrue(event.getChanges().containsKey("name"));
		assertTrue(event.getChanges().containsKey("phone"));
		assertTrue(event.getChanges().containsKey("businessAddress"));
	}

	@Test
	public void testUpdateVendorStatus_VendorNotFound() {
		UUID nonExistentId = UUID.randomUUID();
		when(vendorRepository.findById(nonExistentId)).thenReturn(Mono.empty());

		StepVerifier.create(vendorService.updateVendorStatus(nonExistentId, Vendor.VendorStatus.SUSPENDED, "Reason"))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorRepository, never()).save(any(Vendor.class));
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testUpdateVendorStatus_Success() {
		Vendor existingVendor = Vendor.builder()
				.id(vendorId)
				.vendorStatus(Vendor.VendorStatus.ACTIVE)
				.build();

		Vendor updatedVendor = Vendor.builder()
				.id(vendorId)
				.vendorStatus(Vendor.VendorStatus.SUSPENDED)
				.build();

		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(existingVendor));
		when(vendorRepository.save(any(Vendor.class))).thenReturn(Mono.just(updatedVendor));
		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(vendorService.updateVendorStatus(vendorId, Vendor.VendorStatus.SUSPENDED, "Violation"))
				.expectNextMatches(v -> v.getVendorStatus() == Vendor.VendorStatus.SUSPENDED)
				.verifyComplete();

		verify(eventPublisher).publish(statusChangedEventCaptor.capture());
		VendorStatusChangedEvent event = statusChangedEventCaptor.getValue();
		assertEquals(vendorId, event.getVendorId());
		assertEquals(Vendor.VendorStatus.SUSPENDED.toString(), event.getNewStatus());
		assertEquals("Violation", event.getReason());
	}

	@Test
	public void testUpdateVerificationStatus_InvalidStatus() {
		try (MockedStatic<VendorValidator> mockedValidator = mockStatic(VendorValidator.class)) {
			mockedValidator.when(() -> isValidVerificationStatus(any(Vendor.VerificationStatus.class)))
					.thenReturn(true);

			StepVerifier.create(vendorService.updateVerificationStatus(vendorId, Vendor.VerificationStatus.PENDING))
					.expectError(ValidationException.class)
					.verify();

			verify(vendorRepository, never()).findById(any(UUID.class));
			verify(vendorRepository, never()).save(any(Vendor.class));
			verify(eventPublisher, never()).publish(any());
		}
	}

	@Test
	public void testUpdateVerificationStatus_VendorNotFound() {
		UUID nonExistentId = UUID.randomUUID();
		when(vendorRepository.findById(nonExistentId)).thenReturn(Mono.empty());

		StepVerifier.create(vendorService.updateVerificationStatus(nonExistentId, Vendor.VerificationStatus.VERIFIED))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorRepository, never()).save(any(Vendor.class));
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testUpdateVerificationStatus_Success() {
		Vendor existingVendor = Vendor.builder()
				.id(vendorId)
				.verificationStatus(Vendor.VerificationStatus.PENDING)
				.build();

		Vendor updatedVendor = Vendor.builder()
				.id(vendorId)
				.verificationStatus(Vendor.VerificationStatus.VERIFIED)
				.build();

		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(existingVendor));
		when(vendorRepository.save(any(Vendor.class))).thenReturn(Mono.just(updatedVendor));
		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(vendorService.updateVerificationStatus(vendorId, Vendor.VerificationStatus.VERIFIED))
				.expectNextMatches(v -> v.getVerificationStatus() == Vendor.VerificationStatus.VERIFIED)
				.verifyComplete();

		verify(eventPublisher).publish(verificationEventCaptor.capture());
		VendorVerificationCompletedEvent event = verificationEventCaptor.getValue();
		assertEquals(vendorId, event.getVendorId());
		assertEquals(Vendor.VerificationStatus.VERIFIED.toString(), event.getVerificationStatus());
	}

	@Test
	public void testDeactivateVendor_VendorNotFound() {
		UUID nonExistentId = UUID.randomUUID();
		when(vendorRepository.findById(nonExistentId)).thenReturn(Mono.empty());

		StepVerifier.create(vendorService.deactivateVendor(nonExistentId))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorRepository, never()).save(any(Vendor.class));
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	public void testDeactivateVendor_Success() {
		Vendor activeVendor = Vendor.builder()
				.id(vendorId)
				.active(true)
				.build();

		Vendor deactivatedVendor = Vendor.builder()
				.id(vendorId)
				.active(false)
				.build();

		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(activeVendor));
		when(vendorRepository.save(any(Vendor.class))).thenReturn(Mono.just(deactivatedVendor));
		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(vendorService.deactivateVendor(vendorId))
				.verifyComplete();

		verify(vendorRepository).save(any(Vendor.class));
		verify(eventPublisher).publish(statusChangedEventCaptor.capture());
		VendorStatusChangedEvent event = statusChangedEventCaptor.getValue();
		assertEquals(vendorId, event.getVendorId());
		assertFalse(deactivatedVendor.getActive());
	}

	@Test
	public void testUpdateMultipleFields() {
		Vendor existingVendor = Vendor.builder()
				.id(vendorId)
				.email("test@example.com")
				.name("Old Name")
				.businessName("Old Business")
				.taxId("OLD-TAX")
				.build();

		Address newAddress = Address.builder()
				.street("New Street")
				.buildingNumber("12")
				.city("New City")
				.country("New Country")
				.build();

		Vendor update = Vendor.builder()
				.id(vendorId)
				.name("New Name")
				.businessName("New Business")
				.taxId("NEW-TAX")
				.phone("555-1234")
				.businessAddress(newAddress)
				.build();

		Vendor updatedVendor = Vendor.builder()
				.id(vendorId)
				.email("test@example.com")
				.name("New Name")
				.businessName("New Business")
				.taxId("NEW-TAX")
				.phone("555-1234")
				.businessAddress(newAddress)
				.build();

		when(vendorRepository.findById(vendorId)).thenReturn(Mono.just(existingVendor));
		when(vendorRepository.save(any(Vendor.class))).thenReturn(Mono.just(updatedVendor));
		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(vendorService.updateVendor(vendorId, update))
				.expectNextMatches(v ->
						v.getName().equals("New Name") &&
								v.getBusinessName().equals("New Business") &&
								v.getTaxId().equals("NEW-TAX") &&
								v.getPhone().equals("555-1234") &&
								v.getBusinessAddress().getStreet().equals("New Street"))
				.verifyComplete();

		verify(eventPublisher).publish(updatedEventCaptor.capture());
		VendorUpdatedEvent event = updatedEventCaptor.getValue();
		Map<String, Object> changes = event.getChanges();
		assertEquals(5, changes.size());
		assertTrue(changes.containsKey("name"));
		assertTrue(changes.containsKey("businessName"));
		assertTrue(changes.containsKey("taxId"));
		assertTrue(changes.containsKey("phone"));
		assertTrue(changes.containsKey("businessAddress"));
	}
}