package pl.ecommerce.vendor.integration.helper;

import pl.ecommerce.vendor.domain.model.Address;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.model.VendorPayment;
import pl.ecommerce.vendor.domain.model.VerificationDocument;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.domain.repository.VendorPaymentRepository;
import pl.ecommerce.vendor.domain.repository.VerificationDocumentRepository;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import pl.ecommerce.vendor.domain.model.Category;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestUtils {

	public static Address createTestAddress() {
		return Address.builder()
				.street("123 Main St")
				.buildingNumber("1")
				.city("Test City")
				.state("Test State")
				.postalCode("12345")
				.country("Test Country")
				.build();
	}

	public static Vendor createTestVendor(UUID vendorId, String email) {
		return Vendor.builder()
				.id(vendorId)
				.email(email)
				.name("Test Vendor")
				.description("Test Description")
				.phone("123456789")
				.businessName("Test Business")
				.taxId("TAX-123456")
				.businessAddress(createTestAddress())
				.bankAccountDetails("Test Bank Account")
				.vendorStatus(Vendor.VendorStatus.ACTIVE)
				.verificationStatus(Vendor.VerificationStatus.PENDING)
				.active(true)
				.build();
	}

	public static MonetaryAmount createMonetaryAmount(double amount, String currencyCode) {
		return Monetary.getDefaultAmountFactory()
				.setCurrency(currencyCode)
				.setNumber(amount)
				.create();
	}

	public static VendorPayment createTestPayment(UUID paymentId, UUID vendorId,
												  MonetaryAmount amount, String paymentMethod) {
		return VendorPayment.builder()
				.id(paymentId)
				.vendorId(vendorId)
				.amount(amount.getNumber().numberValue(BigDecimal.class))
				.currency(amount.getCurrency().getCurrencyCode())
				.paymentMethod(paymentMethod)
				.status(VendorPayment.VendorPaymentStatus.PENDING)
				.createdAt(LocalDateTime.now())
				.build();
	}

	public static VerificationDocument createTestDocument(UUID documentId, UUID vendorId,
														  VerificationDocument.DocumentType documentType, String documentUrl) {
		return VerificationDocument.builder()
				.id(documentId)
				.vendorId(vendorId)
				.documentType(documentType)
				.documentUrl(documentUrl)
				.status(VerificationDocument.VerificationStatus.PENDING)
				.submittedAt(LocalDateTime.now())
				.build();
	}

	public static Category createTestCategory(UUID categoryId, String name) {
		return Category.builder()
				.id(categoryId)
				.name(name)
				.description("Test Category Description")
				.build();
	}

	public static CategoryAssignment createTestCategoryAssignment(UUID vendorId, Category category,
																  MonetaryAmount commissionRate) {
		return CategoryAssignment.builder()
				.vendorId(vendorId)
				.category(category)
				.status(CategoryAssignment.CategoryAssignmentStatus.ACTIVE)
				.commissionAmount(commissionRate.getNumber().numberValue(BigDecimal.class))
				.commissionCurrency(commissionRate.getCurrency().getCurrencyCode())
				.build();
	}

	public static void cleanRepositories(VendorRepository vendorRepository,
										 VendorPaymentRepository paymentRepository,
										 VerificationDocumentRepository documentRepository,
										 CategoryAssignmentRepository categoryAssignmentRepository) {
		if (vendorRepository != null) {
			vendorRepository.deleteAll().block();
		}
		if (paymentRepository != null) {
			paymentRepository.deleteAll().block();
		}
		if (documentRepository != null) {
			documentRepository.deleteAll().block();
		}
		if (categoryAssignmentRepository != null) {
			categoryAssignmentRepository.deleteAll().block();
		}
	}
}