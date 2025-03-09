package pl.ecommerce.vendor.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.api.dto.CategoryAssignmentRequest;
import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Category;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import pl.ecommerce.vendor.infrastructure.client.ProductServiceClient;
import pl.ecommerce.vendor.infrastructure.exception.CategoryAssignmentException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryAssignmentRepository categoryAssignmentRepository;

	@Mock
	private VendorService vendorService;

	@Mock
	private ProductServiceClient productServiceClient;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private CategoryService categoryService;

	private UUID vendorId;
	private UUID categoryId1;
	private UUID categoryId2;
	private Vendor vendor;
	private CategoryAssignment categoryAssignment1;
	private CategoryAssignment categoryAssignment2;
	private MonetaryAmount commissionRate;
	private List<CategoryAssignmentRequest> categoryRequests;
	private List<ProductServiceClient.CategoryResponse> categoryResponses;

	@BeforeEach
	void setUp() {
		vendorId = UUID.randomUUID();
		categoryId1 = UUID.randomUUID();
		categoryId2 = UUID.randomUUID();
		commissionRate = Monetary.getDefaultAmountFactory()
				.setCurrency("PLN")
				.setNumber(0.1)
				.create();

		Address businessAddress = Address.builder()
				.street("Test Street")
				.buildingNumber("123")
				.city("Test City")
				.state("Test State")
				.postalCode("12-345")
				.country("Poland")
				.build();

		vendor = Vendor.builder()
				.id(vendorId)
				.email("test@example.com")
				.name("Test Vendor")
				.description("Test Description")
				.phone("+48123456789")
				.businessName("Test Business")
				.taxId("PL1234567890")
				.businessAddress(businessAddress)
				.bankAccountDetails("PL12345678901234567890123456")
				.active(true)
				.vendorStatus(Vendor.VendorStatus.ACTIVE)
				.verificationStatus(Vendor.VerificationStatus.VERIFIED)
				.build();

		Category category1 = Category.builder()
				.id(categoryId1)
				.name("Electronics")
				.description("Electronic devices and accessories")
				.build();

		Category category2 = Category.builder()
				.id(categoryId2)
				.name("Home")
				.description("Home and garden products")
				.build();

		categoryAssignment1 = CategoryAssignment.builder()
				.id(UUID.randomUUID())
				.vendorId(vendorId)
				.category(category1)
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.assignedAt(LocalDateTime.now())
				.build();

		categoryAssignment2 = CategoryAssignment.builder()
				.id(UUID.randomUUID())
				.vendorId(vendorId)
				.category(category2)
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.assignedAt(LocalDateTime.now())
				.build();

		categoryRequests = new ArrayList<>();
		categoryRequests.add(new CategoryAssignmentRequest(categoryId1.toString(), commissionRate));
		categoryRequests.add(new CategoryAssignmentRequest(categoryId2.toString(), commissionRate));

		ProductServiceClient.CategoryResponse categoryResponse1 =
				new ProductServiceClient.CategoryResponse(
						categoryId1,
						"Electronics",
						"Electronic devices and accessories");

		ProductServiceClient.CategoryResponse categoryResponse2 =
				new ProductServiceClient.CategoryResponse(
						categoryId2,
						"Home",
						"Home and garden products");

		categoryResponses = List.of(categoryResponse1, categoryResponse2);
	}

	@Test
	void assignCategories_Success() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(categoryAssignmentRepository.existsByVendorIdAndCategoryId(eq(vendorId), any(UUID.class)))
				.thenReturn(Mono.just(false));
		when(productServiceClient.getCategories(anyList())).thenReturn(Flux.fromIterable(categoryResponses));
		when(categoryAssignmentRepository.saveAll(anyList()))
				.thenReturn(Flux.just(categoryAssignment1, categoryAssignment2));

		doNothing().when(eventPublisher).publish(any());

		StepVerifier.create(categoryService.assignCategories(vendorId, categoryRequests))
				.expectNext(categoryAssignment1, categoryAssignment2)
				.verifyComplete();

		verify(vendorService).getVendorById(vendorId);
		verify(categoryAssignmentRepository, times(2)).existsByVendorIdAndCategoryId(eq(vendorId), any(UUID.class));
		verify(productServiceClient).getCategories(anyList());
		verify(categoryAssignmentRepository).saveAll(anyList());
		verify(eventPublisher).publish(any());
	}

	@Test
	void assignCategories_VendorNotFound() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.empty());

		StepVerifier.create(categoryService.assignCategories(vendorId, categoryRequests))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorService).getVendorById(vendorId);
		verify(categoryAssignmentRepository, never()).existsByVendorIdAndCategoryId(any(), any());
		verify(productServiceClient, never()).getCategories(anyList());
		verify(categoryAssignmentRepository, never()).saveAll(anyList());
	}

	@Test
	void assignCategories_VendorInactive() {
		vendor.setActive(false);
		var categoryResponse = ProductServiceClient.CategoryResponse.builder()
				.id(UUID.randomUUID())
				.name("test")
				.description("test")
				.build();

		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(productServiceClient.getCategories(anyList())).thenReturn(Flux.just(categoryResponse));

		StepVerifier.create(categoryService.assignCategories(vendorId, categoryRequests))
				.expectError(CategoryAssignmentException.class)
				.verify();

		verify(vendorService).getVendorById(vendorId);
		verify(categoryAssignmentRepository, never()).existsByVendorIdAndCategoryId(any(), any());
		verify(categoryAssignmentRepository, never()).saveAll(anyList());
	}

	@Test
	void assignCategories_CategoryAlreadyAssigned() {
		var categoryResponse = ProductServiceClient.CategoryResponse.builder()
				.id(UUID.randomUUID())
				.name("test")
				.description("test")
				.build();

		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(productServiceClient.getCategories(anyList())).thenReturn(Flux.just(categoryResponse));
		when(categoryAssignmentRepository.existsByVendorIdAndCategoryId(eq(vendorId), any(UUID.class)))
				.thenReturn(Mono.just(true));

		StepVerifier.create(categoryService.assignCategories(vendorId, categoryRequests))
				.expectError(CategoryAssignmentException.class)
				.verify();

		verify(vendorService).getVendorById(vendorId);
		verify(categoryAssignmentRepository).existsByVendorIdAndCategoryId(eq(vendorId), any(UUID.class));
		verify(categoryAssignmentRepository, never()).saveAll(anyList());
	}

	@Test
	void assignCategories_CategoryNotFound() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(categoryAssignmentRepository.existsByVendorIdAndCategoryId(eq(vendorId), any(UUID.class)))
				.thenReturn(Mono.just(false));

		when(productServiceClient.getCategories(anyList())).thenReturn(Flux.just(categoryResponses.getFirst()));

		StepVerifier.create(categoryService.assignCategories(vendorId, categoryRequests))
				.expectError(CategoryAssignmentException.class)
				.verify();

		verify(vendorService).getVendorById(vendorId);
		verify(categoryAssignmentRepository, times(2)).existsByVendorIdAndCategoryId(eq(vendorId), any(UUID.class));
		verify(productServiceClient).getCategories(anyList());
		verify(categoryAssignmentRepository, never()).saveAll(anyList());
	}

	@Test
	void getVendorCategories_Success() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(categoryAssignmentRepository.findByVendorId(vendorId))
				.thenReturn(Flux.just(categoryAssignment1, categoryAssignment2));

		StepVerifier.create(categoryService.getVendorCategories(vendorId))
				.expectNext(categoryAssignment1, categoryAssignment2)
				.verifyComplete();

		verify(vendorService).getVendorById(vendorId);
		verify(categoryAssignmentRepository).findByVendorId(vendorId);
	}

	@Test
	void getVendorCategories_VendorNotFound() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.empty());

		StepVerifier.create(categoryService.getVendorCategories(vendorId))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorService).getVendorById(vendorId);
	}

	@Test
	void getVendorActiveCategories_Success() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.just(vendor));
		when(categoryAssignmentRepository.findByVendorIdAndStatus(
				vendorId, CategoryAssignment.CategoryAssignmentStatus.ACTIVE))
				.thenReturn(Flux.just(categoryAssignment1, categoryAssignment2));

		StepVerifier.create(categoryService.getVendorActiveCategories(vendorId))
				.expectNext(categoryAssignment1, categoryAssignment2)
				.verifyComplete();

		verify(vendorService).getVendorById(vendorId);
		verify(categoryAssignmentRepository).findByVendorIdAndStatus(
				vendorId, CategoryAssignment.CategoryAssignmentStatus.ACTIVE);
	}

	@Test
	void getVendorActiveCategories_VendorNotFound() {
		when(vendorService.getVendorById(vendorId)).thenReturn(Mono.empty());

		StepVerifier.create(categoryService.getVendorActiveCategories(vendorId))
				.expectError(VendorNotFoundException.class)
				.verify();

		verify(vendorService).getVendorById(vendorId);
	}

	@Test
	void updateCategoryStatus_Success() {
		when(categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId1))
				.thenReturn(Mono.just(categoryAssignment1));
		when(categoryAssignmentRepository.save(any(CategoryAssignment.class)))
				.thenReturn(Mono.just(categoryAssignment1));

		StepVerifier.create(categoryService.updateCategoryStatus(
						vendorId, categoryId1, CategoryAssignment.CategoryAssignmentStatus.INACTIVE))
				.expectNext(categoryAssignment1)
				.verifyComplete();

		verify(categoryAssignmentRepository).findByVendorIdAndCategoryId(vendorId, categoryId1);
		verify(categoryAssignmentRepository).save(any(CategoryAssignment.class));
	}

	@Test
	void updateCategoryStatus_CategoryNotFound() {
		when(categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId1))
				.thenReturn(Mono.empty());

		StepVerifier.create(categoryService.updateCategoryStatus(
						vendorId, categoryId1, CategoryAssignment.CategoryAssignmentStatus.INACTIVE))
				.expectError(CategoryAssignmentException.class)
				.verify();

		verify(categoryAssignmentRepository).findByVendorIdAndCategoryId(vendorId, categoryId1);
		verify(categoryAssignmentRepository, never()).save(any(CategoryAssignment.class));
	}

	@Test
	void removeCategory_Success() {
		when(categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId1))
				.thenReturn(Mono.just(categoryAssignment1));
		when(categoryAssignmentRepository.delete(any(CategoryAssignment.class)))
				.thenReturn(Mono.empty());

		StepVerifier.create(categoryService.removeCategory(vendorId, categoryId1))
				.verifyComplete();

		verify(categoryAssignmentRepository).findByVendorIdAndCategoryId(vendorId, categoryId1);
		verify(categoryAssignmentRepository).delete(any(CategoryAssignment.class));
	}

	@Test
	void removeCategory_CategoryNotFound() {
		when(categoryAssignmentRepository.findByVendorIdAndCategoryId(vendorId, categoryId1))
				.thenReturn(Mono.empty());

		StepVerifier.create(categoryService.removeCategory(vendorId, categoryId1))
				.expectError(CategoryAssignmentException.class)
				.verify();

		verify(categoryAssignmentRepository).findByVendorIdAndCategoryId(vendorId, categoryId1);
		verify(categoryAssignmentRepository, never()).delete(any(CategoryAssignment.class));
	}
}