package pl.ecommerce.vendor.write.infrastructure.exception;

import java.util.UUID;

public class CategoryAlreadyAssignedException extends RuntimeException {

	public CategoryAlreadyAssignedException(UUID categoryId) {
		super("Category with ID " + categoryId + " is already assigned to this vendor");
	}
}