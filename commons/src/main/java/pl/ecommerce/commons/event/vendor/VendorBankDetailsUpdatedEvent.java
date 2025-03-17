package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;

import java.time.Instant;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.bank-details-updated.event")
public class VendorBankDetailsUpdatedEvent extends VendorEvent {
	private String bankAccountNumber;
	private String bankName;
	private String bankSwiftCode;

	@Builder
	public VendorBankDetailsUpdatedEvent(UUID vendorId, String bankAccountNumber,
										 String bankName, String bankSwiftCode,
										 Instant timestamp, int version) {
		super(vendorId, version, timestamp);
		this.bankAccountNumber = bankAccountNumber;
		this.bankName = bankName;
		this.bankSwiftCode = bankSwiftCode;
	}
}