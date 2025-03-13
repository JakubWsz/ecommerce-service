package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.verification.completed.event")
@NoArgsConstructor
public class VendorVerificationCompletedEvent extends VendorEvent {
	private String verificationStatus;
	private LocalDateTime verificationTimestamp;

	@Builder
	public VendorVerificationCompletedEvent(UUID correlationId, UUID vendorId, String verificationStatus,
											LocalDateTime verificationTimestamp, int version, Instant timestamp) {
		super(vendorId, version, timestamp);
		this.verificationStatus = verificationStatus;
		this.verificationTimestamp = verificationTimestamp;
	}
}
