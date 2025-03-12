package pl.ecommerce.product.api;

import pl.ecommerce.product.api.dto.CategoryResponse;
import pl.ecommerce.product.api.dto.ProductImageResponse;
import pl.ecommerce.product.api.dto.ProductResponse;
import pl.ecommerce.product.domain.model.Category;
import pl.ecommerce.product.domain.model.Product;
import pl.ecommerce.product.domain.model.ProductImage;

import java.util.stream.Collectors;

public interface DtoMapper {

	static ProductResponse map(Product product) {
		return ProductResponse.builder()
				.id(product.getId())
				.name(product.getName())
				.description(product.getDescription())
				.price(product.getPrice())
				.stock(product.getStock())
				.active(product.isActive())
				.categorises(product.getCategories().stream()
						.map(DtoMapper::map)
						.collect(Collectors.toSet()))
				.attributes(product.getAttributes())
				.createdAt(product.getCreatedAt())
				.updatedAt(product.getUpdatedAt())
				.imageUrls(product.getImages().stream()
						.map(DtoMapper::map)
						.collect(Collectors.toSet()))
				.build();
	}

	static CategoryResponse map(Category category) {
		return CategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.description(category.getDescription())
				.parent(category.getParent())
				.children(category.getChildren())
				.active(category.isActive())
				.createdAt(category.getCreatedAt())
				.updatedAt(category.getUpdatedAt())
				.build();
	}

	static ProductImageResponse map(ProductImage image) {
		return ProductImageResponse.builder()
				.id(image.getId())
				.product(image.getProduct())
				.url(image.getUrl())
				.sortOrder(image.getSortOrder())
				.createdAt(image.getCreatedAt())
				.build();
	}
}
