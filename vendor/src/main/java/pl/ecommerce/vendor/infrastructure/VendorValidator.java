package pl.ecommerce.vendor.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorValidator {


	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

	private static final Pattern PHONE_PATTERN =
			Pattern.compile("^\\+?[0-9]{8,15}$");


	public static Vendor initializeNewVendor(Vendor requestVendor) {
		UUID vendorId = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();

		return Vendor.builder()
				.id(vendorId)
				.name(requestVendor.getName())
				.description(requestVendor.getDescription())
				.email(requestVendor.getEmail())
				.phone(requestVendor.getPhone())
				.businessName(requestVendor.getBusinessName())
				.taxId(requestVendor.getTaxId())
				.businessAddress(requestVendor.getBusinessAddress())
				.bankAccountDetails(requestVendor.getBankAccountDetails())
				.registrationDate(now)
				.createdAt(now)
				.updatedAt(now)
				.gdprConsent(requestVendor.getGdprConsent())
				.consentTimestamp(requestVendor.getGdprConsent() ? now : null)
				.active(true)
				.build();
	}

	public static Mono<Void> validateVendor(Vendor vendor) {
		if (vendor.getEmail() == null || !EMAIL_PATTERN.matcher(vendor.getEmail()).matches()) {
			return Mono.error(new ValidationException("Invalid email format"));
		}

		if (vendor.getPhone() != null && !PHONE_PATTERN.matcher(vendor.getPhone()).matches()) {
			return Mono.error(new ValidationException("Invalid phone number format"));
		}

		if (vendor.getName() == null || vendor.getName().trim().isEmpty()) {
			return Mono.error(new ValidationException("Vendor name is required"));
		}

		if (vendor.getBusinessName() == null || vendor.getBusinessName().trim().isEmpty()) {
			return Mono.error(new ValidationException("Business name is required"));
		}

		return validateGdprConsent(vendor);
	}

	public static Vendor activateVendor(Vendor vendor) {
		if (!vendor.isVerified()) {
			throw new ValidationException("Cannot activate unverified vendor");
		}

		vendor.setVendorStatus(Vendor.VendorStatus.valueOf("ACTIVE"));
		vendor.setUpdatedAt(LocalDateTime.now());
		return vendor;
	}

	public static Vendor suspendVendor(Vendor vendor, String reason) {
		vendor.setVendorStatus(Vendor.VendorStatus.valueOf("SUSPENDED"));
		vendor.setUpdatedAt(LocalDateTime.now());
		//todo publish
		return vendor;
	}

	public static Vendor banVendor(Vendor vendor, String reason) {
		vendor.setVendorStatus(Vendor.VendorStatus.valueOf("BANNED"));
		vendor.setActive(false);
		vendor.setUpdatedAt(LocalDateTime.now());
		//todo publish

		return vendor;
	}

	public static Vendor updateVerificationStatus(Vendor vendor, Vendor.VendorVerificationStatus status) {
		vendor.setVerificationVendorStatus(status);
		vendor.setUpdatedAt(LocalDateTime.now());

		if (Vendor.VendorVerificationStatus.VERIFIED.equals(status) && vendor.isPending()) {
			vendor.setVendorStatus(Vendor.VendorStatus.ACTIVE);
		}

		return vendor;
	}

	public static Mono<Void> validateGdprConsent(Vendor vendor) {
		if (!vendor.getGdprConsent()) {
			return Mono.error(new ValidationException("GDPR consent is required"));
		}
		return Mono.empty();
	}

	public static boolean isValidVerificationStatus(Vendor.VendorVerificationStatus status) {
		return !EnumSet.of(
				Vendor.VendorVerificationStatus.PENDING,
				Vendor.VendorVerificationStatus.VERIFIED,
				Vendor.VendorVerificationStatus.REJECTED
		).contains(status);
	}
}

