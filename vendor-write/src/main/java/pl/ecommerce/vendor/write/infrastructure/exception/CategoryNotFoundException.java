package pl.ecommerce.vendor.write.infrastructure.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

	public CategoryNotFoundException(UUID categoryId) {
		super("Category not found with ID: " + categoryId);
	}
}