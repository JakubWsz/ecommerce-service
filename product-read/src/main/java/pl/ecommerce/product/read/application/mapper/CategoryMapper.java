package pl.ecommerce.product.read.application.mapper;

import lombok.experimental.UtilityClass;
import pl.ecommerce.product.read.api.dto.CategoryResponse;
import pl.ecommerce.product.read.api.dto.CategorySummary;
import pl.ecommerce.product.read.domain.model.CategoryReadModel;

import java.util.Collections;

@UtilityClass
public class CategoryMapper {

	public static CategoryResponse toCategoryResponse(CategoryReadModel category) {
		return CategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.description(category.getDescription())
				.slug(category.getSlug())
				.parentCategoryId(category.getParentCategoryId())
				.parentCategoryName(category.getParentCategoryName())
				.subcategoryIds(category.getSubcategoryIds())
				.subcategoryNames(category.getSubcategoryNames())
				.attributes(category.getAttributes())
				.active(category.isActive())
				.iconUrl(category.getIconUrl())
				.imageUrl(category.getImageUrl())
				.displayOrder(category.getDisplayOrder())
				.productCount(category.getProductCount())
				.createdAt(category.getCreatedAt())
				.updatedAt(category.getUpdatedAt())
				.children(Collections.emptyList())
				.build();
	}

	public static CategorySummary toCategorySummary(CategoryReadModel category) {
		return CategorySummary.builder()
				.id(category.getId())
				.name(category.getName())
				.slug(category.getSlug())
				.imageUrl(category.getImageUrl())
				.parentCategoryId(category.getParentCategoryId())
				.productCount(category.getProductCount())
				.childCount(category.getSubcategoryIds() != null ? category.getSubcategoryIds().size() : 0)
				.active(category.isActive())
				.displayOrder(category.getDisplayOrder())
				.build();
	}
}
