package pl.ecommerce.vendor.domain.model;

import lombok.*;
import org.javamoney.moneta.Money;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@ToString
@SuperBuilder
@Getter
@Document(collection = "vendor_categories")
@CompoundIndex(name = "vendor_category_idx", def = "{'vendorId': 1, 'category.id': 1}", unique = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryAssignment extends BaseEntity {

	@Field("vendorId")
	private UUID vendorId;

	@Field("category")
	private Category category;

	@Setter
	@Field("status")
	private CategoryAssignmentStatus status;

	@Field("commissionAmount")
	@Getter(AccessLevel.NONE)
	private BigDecimal commissionAmount;

	@Field("commissionCurrency")
	@Getter(AccessLevel.NONE)
	private String commissionCurrency;

	@Field("assignedAt")
	private LocalDateTime assignedAt;

	@Setter
	@Field("statusChangeReason")
	private String statusChangeReason;

	public static CategoryAssignment create(UUID vendorId, Category category, MonetaryAmount commissionRate) {
		CategoryAssignment assignment = CategoryAssignment.builder()
				.vendorId(vendorId)
				.category(category)
				.status(CategoryAssignmentStatus.ACTIVE)
				.assignedAt(LocalDateTime.now())
				.build();

		assignment.setCategoryCommissionRate(commissionRate);
		return assignment;
	}


	public MonetaryAmount getCategoryCommissionRate() {
		if (commissionAmount == null || commissionCurrency == null) {
			return null;
		}
		return Money.of(commissionAmount, commissionCurrency);
	}

	private void setCategoryCommissionRate(MonetaryAmount monetaryAmount) {
		if (monetaryAmount == null) {
			this.commissionAmount = null;
			this.commissionCurrency = null;
		} else {
			this.commissionAmount = monetaryAmount.getNumber().numberValue(BigDecimal.class);
			this.commissionCurrency = monetaryAmount.getCurrency().getCurrencyCode();
		}
	}

	public boolean isActive() {
		return CategoryAssignmentStatus.ACTIVE.equals(status);
	}

	public MonetaryAmount getEffectiveCommissionRate(MonetaryAmount vendorDefaultRate) {
		return getCategoryCommissionRate() != null ? getCategoryCommissionRate() : vendorDefaultRate;
	}

	public boolean canTransitionTo(CategoryAssignmentStatus newStatus) {
		return true;
	}

	public enum CategoryAssignmentStatus {
		ACTIVE, INACTIVE
	}

}