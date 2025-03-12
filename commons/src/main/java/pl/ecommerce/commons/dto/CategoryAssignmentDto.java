package pl.ecommerce.commons.dto;

import lombok.Builder;
import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CategoryAssignmentDto(
		UUID id,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		String createdBy,
		String updatedBy,
		UUID vendorId,
		CategoryDto category,
		CategoryAssignmentStatusDto status,
		BigDecimal categoryCommissionRate,
		String currencyUnit,
		LocalDateTime assignedAt,
		String statusChangeReason

) {
	public enum CategoryAssignmentStatusDto {
		ACTIVE, INACTIVE
	}

	@Builder
	public record CategoryDto(
			UUID id,
			String name,
			String description
	) {
	}
}
