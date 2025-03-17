package pl.ecommerce.vendor.read.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import pl.ecommerce.commons.model.vendor.VendorStatus;

import java.time.Instant;
import java.util.UUID;

@Builder
@Data
@Schema(description = "Summary information of a vendor")
public class VendorSummary {

	@Schema(description = "Unique identifier of the vendor", example = "e7b8c2d5-0d07-4f28-9e0b-8b68b4e68d9a")
	private UUID id;

	@Schema(description = "Vendor's name", example = "Tech Solutions Inc")
	private String name;

	@Schema(description = "Vendor's business name", example = "Tech Solutions Inc Ltd.")
	private String businessName;

	@Schema(description = "Vendor's email address", example = "contact@techsolutions.com")
	private String email;

	@Schema(description = "Vendor status", example = "ACTIVE")
	private VendorStatus status;

	@Schema(description = "Indicates if the vendor is verified", example = "true")
	private boolean verified;

	@Schema(description = "Number of categories assigned to the vendor", example = "5")
	private int categoryCount;

	@Schema(description = "Timestamp when the vendor was created", example = "2023-01-01T12:00:00Z")
	private Instant createdAt;

	@Schema(description = "Trace identifier", example = "12345678-1234-1234-1234-1234567890ab")
	private String traceId;
}