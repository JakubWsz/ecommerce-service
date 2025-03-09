package pl.ecommerce.vendor.api.mapper;

import pl.ecommerce.vendor.api.dto.CategoryAssignmentResponse;
import pl.ecommerce.vendor.domain.model.CategoryAssignment;

public class CategoryAssignmentMapper {

	private CategoryAssignmentMapper() {
	}

	public static CategoryAssignmentResponse toResponse(CategoryAssignment assignment) {
		return CategoryAssignmentResponse.builder()
				.id(assignment.getId())
				.vendorId(assignment.getVendorId())
				.categoryId(assignment.getCategory().getId())
				.categoryName(assignment.getCategory().getName())
				.status(String.valueOf(assignment.getStatus()))
				.categoryCommissionRate(assignment.getCategoryCommissionRate())
				.assignedAt(assignment.getAssignedAt())
				.createdAt(assignment.getCreatedAt())
				.updatedAt(assignment.getUpdatedAt())
				.build();
	}
}