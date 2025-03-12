package pl.ecommerce.vendor.integration;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import pl.ecommerce.commons.event.vendor.VendorCategoriesAssignedEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.api.dto.CategoryAssignmentRequest;
import pl.ecommerce.vendor.domain.model.Category;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.service.CategoryService;
import pl.ecommerce.vendor.domain.service.VendorService;
import pl.ecommerce.vendor.infrastructure.client.ProductServiceClient;
import pl.ecommerce.vendor.infrastructure.exception.CategoryAssignmentException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import pl.ecommerce.vendor.integration.helper.TestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ActiveProfiles("test")
class CategoryServiceIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private VendorRepository vendorRepository;

	@Autowired
	private CategoryAssignmentRepository categoryAssignmentRepository;

	@Autowired
	private EventPublisher eventPublisher;

	@Mock
	private ProductServiceClient productServiceClient;

	private VendorService vendorService;
	private CategoryService categoryService;

	private static final UUID VENDOR_ID = UUID.randomUUID();
	private static final UUID CATEGORY_ID_1 = UUID.randomUUID();
	private static final UUID CATEGORY_ID_2 = UUID.randomUUID();
	private static final String VENDOR_EMAIL = "test.vendor@example.com";
	private Vendor testVendor;
	private List<ProductServiceClient.CategoryResponse> categoryResponses;
	private MonetaryAmount commissionRate;

	@BeforeEach
	void setupBeforeEach() {
		TestUtils.cleanRepositories(vendorRepository, null, null, categoryAssignmentRepository);

		testVendor = TestUtils.createTestVendor(VENDOR_ID, VENDOR_EMAIL);
		vendorRepository.save(testVendor).block();

		vendorService = new VendorService(vendorRepository, eventPublisher);
		categoryService = new CategoryService(categoryAssignmentRepository, vendorService,
				productServiceClient, eventPublisher);

		commissionRate = TestUtils.createMonetaryAmount(10.0, "USD");
		categoryResponses = List.of(
				new ProductServiceClient.CategoryResponse(
						CATEGORY_ID_1,
						"Electronics",
						"Electronic devices and accessories"),
				new ProductServiceClient.CategoryResponse(
						CATEGORY_ID_2,
						"Home & Garden",
						"Home improvement and garden supplies")
		);
	}

	@Test
	void assignCategories_WithValidData_ShouldAssignSuccessfully() {

		ProductServiceClient.CategoryResponse categoryResponse = categoryResponses.getFirst();
		Mockito.reset(productServiceClient);
		when(productServiceClient.getCategories(Mockito.argThat(list ->
				list.size() == 1 && list.contains(CATEGORY_ID_1))))
				.thenReturn(Flux.just(categoryResponse));

		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		StepVerifier.create(categoryService.assignCategories(VENDOR_ID, requests).collectList())
				.assertNext(assignments -> {
					assertThat(assignments).hasSize(1);
					assertThat(assignments.getFirst().getVendorId()).isEqualTo(VENDOR_ID);
					assertThat(assignments.getFirst().getCategory().getId()).isEqualTo(CATEGORY_ID_1);
					assertThat(assignments.getFirst().getStatus()).isEqualTo(CategoryAssignment.CategoryAssignmentStatus.ACTIVE);
					assertThat(assignments.getFirst().getCategoryCommissionRate()).isEqualTo(commissionRate);
				})
				.verifyComplete();

		await().atMost(5, TimeUnit.SECONDS)
				.until(() -> testEventListener.getEventCount(VendorCategoriesAssignedEvent.class) > 0);

		StepVerifier.create(testEventListener.getCapturedEventsFlux(VendorCategoriesAssignedEvent.class).next())
				.assertNext(vendorEvent -> {
					var event = vendorEvent.getFirst();
					var categoryAssignmentDto = event.getCategories().getFirst();
					assertThat(event.getVendorId()).isEqualTo(VENDOR_ID);
					assertEquals(categoryResponse.id(), categoryAssignmentDto.category().id());
					assertEquals(categoryResponse.name(), categoryAssignmentDto.category().name());
					assertEquals(categoryResponse.description(), categoryAssignmentDto.category().description());
				})
				.verifyComplete();

		testEventListener.clearEvents();
	}

	@Test
	void assignCategories_ForInactiveVendor_ShouldFail() {
		when(productServiceClient.getCategories(Mockito.anyList()))
				.thenReturn(Flux.fromIterable(categoryResponses));

		testVendor.setActive(false);
		vendorRepository.save(testVendor).block();

		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		StepVerifier.create(categoryService.assignCategories(VENDOR_ID, requests))
				.expectError(CategoryAssignmentException.class)
				.verify();
	}

	@Test
	void assignCategories_ForNonExistingVendor_ShouldFail() {
		UUID nonExistingId = UUID.randomUUID();
		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		StepVerifier.create(categoryService.assignCategories(nonExistingId, requests))
				.expectError(VendorNotFoundException.class)
				.verify();
	}

	@Test
	void assignCategories_WithAlreadyAssignedCategory_ShouldFail() {
		when(productServiceClient.getCategories(Mockito.anyList()))
				.thenReturn(Flux.fromIterable(categoryResponses));

		Category category = TestUtils.createTestCategory(CATEGORY_ID_1, "Electronics");
		CategoryAssignment existingAssignment = TestUtils.createTestCategoryAssignment(VENDOR_ID, category, commissionRate);

		categoryAssignmentRepository.save(existingAssignment).block();

		List<CategoryAssignmentRequest> requests = List.of(
				new CategoryAssignmentRequest(CATEGORY_ID_1.toString(), commissionRate)
		);

		StepVerifier.create(categoryService.assignCategories(VENDOR_ID, requests))
				.expectError(CategoryAssignmentException.class)
				.verify();
	}

	@Test
	void getVendorCategories_ShouldReturnAllCategories() {
		Category category1 = TestUtils.createTestCategory(CATEGORY_ID_1, "Electronics");
		Category category2 = TestUtils.createTestCategory(CATEGORY_ID_2, "Home & Garden");

		CategoryAssignment assignment1 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(category1)
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode()).build();

		CategoryAssignment assignment2 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(category2)
				.status(CategoryAssignment.CategoryAssignmentStatus.INACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.saveAll(List.of(assignment1, assignment2)).blockLast();

		StepVerifier.create(categoryService.getVendorCategories(VENDOR_ID).collectList())
				.assertNext(assignments -> {
					assertThat(assignments).hasSize(2);
					assertThat(assignments.stream().map(a -> a.getCategory().getId()))
							.containsExactlyInAnyOrder(CATEGORY_ID_1, CATEGORY_ID_2);
				})
				.verifyComplete();
	}

	@Test
	void getVendorActiveCategories_ShouldReturnOnlyActiveCategories() {
		Category category1 = TestUtils.createTestCategory(CATEGORY_ID_1, "Electronics");
		Category category2 = TestUtils.createTestCategory(CATEGORY_ID_2, "Home & Garden");

		CategoryAssignment assignment1 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(category1)
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode()).build();

		CategoryAssignment assignment2 = CategoryAssignment.builder()
				.vendorId(VENDOR_ID)
				.category(category2)
				.status(CategoryAssignment.CategoryAssignmentStatus.INACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();

		categoryAssignmentRepository.saveAll(List.of(assignment1, assignment2)).blockLast();

		StepVerifier.create(categoryService.getVendorActiveCategories(VENDOR_ID).collectList())
				.assertNext(assignments -> {
					assertThat(assignments).hasSize(1);
					assertThat(assignments.getFirst().getCategory().getId()).isEqualTo(CATEGORY_ID_1);
					assertThat(assignments.getFirst().getStatus()).isEqualTo(CategoryAssignment.CategoryAssignmentStatus.ACTIVE);
				})
				.verifyComplete();
	}

	@Test
	void updateCategoryStatus_ShouldUpdateStatusSuccessfully() {
		Category category = TestUtils.createTestCategory(CATEGORY_ID_1, "Electronics");
		CategoryAssignment assignment = TestUtils.createTestCategoryAssignment(VENDOR_ID, category, commissionRate);

		categoryAssignmentRepository.save(assignment).block();

		StepVerifier.create(categoryService.updateCategoryStatus(
						VENDOR_ID, CATEGORY_ID_1, CategoryAssignment.CategoryAssignmentStatus.INACTIVE))
				.assertNext(updatedAssignment -> {
					assertThat(updatedAssignment).isNotNull();
					assertThat(updatedAssignment.getVendorId()).isEqualTo(VENDOR_ID);
					assertThat(updatedAssignment.getCategory().getId()).isEqualTo(CATEGORY_ID_1);
					assertThat(updatedAssignment.getStatus()).isEqualTo(CategoryAssignment.CategoryAssignmentStatus.INACTIVE);
				})
				.verifyComplete();
	}

	@Test
	void updateCategoryStatus_WithInvalidStatus_ShouldFail() {
		Category category = TestUtils.createTestCategory(CATEGORY_ID_1, "Electronics");
		CategoryAssignment assignment = TestUtils.createTestCategoryAssignment(VENDOR_ID, category, commissionRate);
		categoryAssignmentRepository.save(assignment).block();

		StepVerifier.create(categoryService.updateCategoryStatus(
						VENDOR_ID, CATEGORY_ID_1, null))
				.expectError(CategoryAssignmentException.class)
				.verify();
	}

	@Test
	void removeCategory_ShouldRemoveSuccessfully() {
		Category category = TestUtils.createTestCategory(CATEGORY_ID_1, "Electronics");
		CategoryAssignment assignment = TestUtils.createTestCategoryAssignment(VENDOR_ID, category, commissionRate);
		categoryAssignmentRepository.save(assignment).block();

		StepVerifier.create(categoryService.removeCategory(VENDOR_ID, CATEGORY_ID_1)
						.then(categoryAssignmentRepository.findByVendorIdAndCategoryId(VENDOR_ID, CATEGORY_ID_1)))
				.verifyComplete();
	}

	@Test
	void removeCategory_NonExistingAssignment_ShouldFail() {
		StepVerifier.create(categoryService.removeCategory(VENDOR_ID, UUID.randomUUID()))
				.expectError(CategoryAssignmentException.class)
				.verify();
	}
}