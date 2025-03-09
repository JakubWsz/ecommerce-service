package pl.ecommerce.vendor.infrastructure.constant;

public interface CategoryConstants extends CommonErrorConstants, CommonLogConstants {

	String ERROR_CATEGORY_ASSIGNMENT_NOT_FOUND = "Category assignment not found";
	String ERROR_CATEGORY_NOT_FOUND = "Category not found: ";
	String ERROR_CATEGORY_FOR_INACTIVE_VENDOR = "Cannot assign category to inactive vendor";
	String ERROR_CATEGORY_ALREADY_ASSIGNED = "Category already assigned to vendor";
	String ERROR_CATEGORIES_NOT_EXISTS = "Not all requested categories exist";
}