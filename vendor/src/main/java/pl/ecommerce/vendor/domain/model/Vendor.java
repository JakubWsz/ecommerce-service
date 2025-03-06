package pl.ecommerce.vendor.domain.model;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

import static pl.ecommerce.vendor.domain.model.Vendor.VendorStatus.*;

@Builder
@Getter
@Document(collection = "vendors")
public class Vendor {

	@Id
	private UUID id;
	@Setter
	private String name;
	@Setter
	private String description;
	@Indexed(unique = true)
	private String email;
	@Setter
	private String phone;
	@Setter
	private String businessName;
	@Setter
	private String taxId;
	@Setter
	private Address businessAddress;
	@Setter
	private String bankAccountDetails;
	private Set<Category> categories;
	@Builder.Default
	@Setter
	private VendorStatus vendorStatus = PENDING;
	@Builder.Default
	@Setter
	private VendorVerificationStatus verificationVendorStatus = VendorVerificationStatus.PENDING;
	private MonetaryAmount commissionRate;
	private LocalDateTime registrationDate;
	@Setter
	private LocalDateTime lastActiveDate;
	private LocalDateTime createdAt;
	@Setter
	private LocalDateTime updatedAt;
	@Builder.Default
	private Boolean gdprConsent = false;
	private LocalDateTime consentTimestamp;
	@Builder.Default
	@Setter
	private Boolean active = true;

	public boolean isVerified() {
		return VendorVerificationStatus.VERIFIED.equals(verificationVendorStatus);
	}

	public boolean isActive() {
		return active && ACTIVE.equals(vendorStatus);
	}

	public boolean isPending() {
		return PENDING.equals(vendorStatus);
	}

	public boolean isSuspended() {
		return SUSPENDED.equals(vendorStatus);
	}

	public boolean isBanned() {
		return BANNED.equals(vendorStatus);
	}

	public enum VendorStatus{
		PENDING, ACTIVE, SUSPENDED, BANNED
	}

	public enum VendorVerificationStatus{
		PENDING, VERIFIED, REJECTED
	}
}
