package pl.ecommerce.vendor.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.UUID;

import static pl.ecommerce.vendor.domain.model.CategoryAssignment.CategoryAssignmentStatus.ACTIVE;

@Builder
@Getter
@Document(collection = "vendor_categories")
public class CategoryAssignment {
	@Id
	private UUID id;
	private UUID vendorId;
	private Category category;
	@Setter
	private CategoryAssignmentStatus status;
	private MonetaryAmount categoryCommissionRate;
	private LocalDateTime assignedAt;
	private LocalDateTime createdAt;
	@Setter
	private LocalDateTime updatedAt;

	public boolean isActive() {
		return ACTIVE.equals(status);
	}

	public MonetaryAmount getEffectiveCommissionRate(MonetaryAmount vendorDefaultRate) {
		return categoryCommissionRate != null ? categoryCommissionRate : vendorDefaultRate;
	}

	public enum CategoryAssignmentStatus{
		ACTIVE, INACTIVE
	}
}