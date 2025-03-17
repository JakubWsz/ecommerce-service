package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;
import pl.ecommerce.commons.model.vendor.VendorStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.verification-completed.event")
public class VendorVerificationCompletedEvent extends VendorEvent {
	private UUID verificationId;
	private VendorStatus verificationStatus;
	private List<String> verifiedFields;

	@Builder
	public VendorVerificationCompletedEvent(UUID vendorId, UUID verificationId,
											VendorStatus verificationStatus, List<String> verifiedFields,
											Instant timestamp, int version) {
		super(vendorId, version, timestamp);
		this.verificationId = verificationId;
		this.verificationStatus = verificationStatus;
		this.verifiedFields = verifiedFields;
	}
}