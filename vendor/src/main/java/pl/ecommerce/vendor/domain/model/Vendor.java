package pl.ecommerce.vendor.domain.model;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

import static pl.ecommerce.vendor.domain.model.Vendor.VendorStatus.*;

import lombok.experimental.SuperBuilder;

import javax.money.CurrencyUnit;

@ToString
@SuperBuilder
@Getter
@Document(collection = "vendors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vendor extends BaseEntity {

	@Setter
	@Field("name")
	private String name;

	@Setter
	@Field("description")
	private String description;

	@Indexed(unique = true)
	@Field("email")
	private String email;

	@Setter
	@Field("phone")
	private String phone;

	@Setter
	@Field("businessName")
	private String businessName;

	@Setter
	@Field("taxId")
	private String taxId;

	@Setter
	@Field("businessAddress")
	private Address businessAddress;

	@Setter
	@Field("bankAccountDetails")
	private String bankAccountDetails;

	@Builder.Default
	@Setter
	@Field("vendorStatus")
	private VendorStatus vendorStatus = PENDING;

	@Builder.Default
	@Setter
	@Field("vendorVerificationStatus")
	private VerificationStatus verificationStatus = VerificationStatus.PENDING;

	@Field("commissionRate")
	private MonetaryAmount commissionRate;

	@Field("defaultCurrency")
	private CurrencyUnit defaultCurrency;

	@Setter
	@Field("lastActiveDate")
	private LocalDateTime lastActiveDate;

	@Builder.Default
	@Field("gdprConsent")
	private Boolean gdprConsent = false;

	@Field("consentTimestamp")
	private LocalDateTime consentTimestamp;

	@Builder.Default
	@Setter
	@Field("active")
	private Boolean active = true;

	@Setter
	@Field("statusChangeReason")
	private String statusChangeReason;

	public boolean isVerified() {
		return VerificationStatus.VERIFIED.equals(verificationStatus);
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

	public boolean canTransitionTo(VendorStatus newStatus) {
		if (this.vendorStatus == BANNED && newStatus != BANNED) {
			return false;
		}

		return true;
	}

	public enum VendorStatus {
		PENDING, ACTIVE, SUSPENDED, BANNED
	}

	public enum VerificationStatus {
		PENDING, VERIFIED, REJECTED
	}
}