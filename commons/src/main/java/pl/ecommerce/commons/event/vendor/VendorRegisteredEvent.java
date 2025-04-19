package pl.ecommerce.commons.event.vendor;

import lombok.*;
import pl.ecommerce.commons.event.Message;
import pl.ecommerce.commons.model.vendor.VendorStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@ToString
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Message("vendor.registered.event")
public class VendorRegisteredEvent extends  VendorEvent {
	private String name;
	private String businessName;
	private String taxId;
	private String email;
	private String phone;
	private String legalForm;
	private Set<UUID> initialCategories;
	private BigDecimal commissionRate;
	private VendorStatus status;

	@Builder
	public VendorRegisteredEvent(UUID vendorId, String name, String businessName,
								 String taxId, String email, String phone,
								 String legalForm, Set<UUID> initialCategories,
								 BigDecimal commissionRate, VendorStatus status,
								 Instant timestamp, int version) {
		super(vendorId, version, timestamp);
		this.name = name;
		this.businessName = businessName;
		this.taxId = taxId;
		this.email = email;
		this.phone = phone;
		this.legalForm = legalForm;
		this.initialCategories = initialCategories;
		this.commissionRate = commissionRate;
		this.status = status;
	}
}

