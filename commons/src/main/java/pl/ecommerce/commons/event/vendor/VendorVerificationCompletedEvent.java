package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.Message;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Message("vendor.verification.completed.event")
@NoArgsConstructor
public class VendorVerificationCompletedEvent extends DomainEvent {
	private UUID vendorId;
	private String verificationStatus;
	private LocalDateTime verificationTimestamp;

	@Builder
	public VendorVerificationCompletedEvent(UUID correlationId, UUID vendorId,
											String verificationStatus,
											LocalDateTime verificationTimestamp) {
		super(correlationId);
		this.vendorId = vendorId;
		this.verificationStatus = verificationStatus;
		this.verificationTimestamp = verificationTimestamp;
	}
}
