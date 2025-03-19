package pl.ecommerce.product.read.application.mapper;

import pl.ecommerce.product.read.api.dto.*;
import pl.ecommerce.product.read.domain.model.ProductReadModel;

import java.util.Collections;
import java.util.stream.Collectors;

public class ProductMapper {

	public static ProductResponse toProductResponse(ProductReadModel product) {
		return ProductResponse.builder()
				.id(product.getId())
				.name(product.getName())
				.description(product.getDescription())
				.sku(product.getSku())
				.categories(product.getCategoryIds())
				.categoryNames(product.getCategoryNames())
				.vendorId(product.getVendorId())
				.vendorName(product.getVendorName())
				.regularPrice(product.getPrice().getRegular())
				.discountedPrice(product.getPrice().getDiscounted())
				.currency(product.getPrice().getCurrency())
				.attributes(product.getAttributes() == null ? Collections.emptyList() :
						product.getAttributes().stream()
								.map(attr -> new ProductAttributeDto(
										attr.getName(), attr.getValue(), attr.getUnit()))
								.collect(Collectors.toList()))
				.variants(product.getVariants() == null ? Collections.emptyList() :
						product.getVariants().stream()
								.map(variant -> ProductVariantDto.builder()
										.id(variant.getId())
										.sku(variant.getSku())
										.attributes(variant.getAttributes().stream()
												.map(attr -> new ProductAttributeDto(
														attr.getName(), attr.getValue(), attr.getUnit()))
												.collect(Collectors.toList()))
										.regularPrice(variant.getPrice().getRegular())
										.discountedPrice(variant.getPrice().getDiscounted())
										.currency(variant.getPrice().getCurrency())
										.stock(new StockInfoDto(
												variant.getStock().getAvailable(),
												variant.getStock().getReserved(),
												variant.getStock().isInStock(),
												variant.getStock().isLowStock()))
										.build())
								.collect(Collectors.toList()))
				.stock(new StockInfoDto(
						product.getStock().getAvailable(),
						product.getStock().getReserved(),
						product.getStock().isInStock(),
						product.getStock().isLowStock()))
				.status(product.getStatus())
				.images(product.getImages())
				.brandName(product.getBrandName())
				.featured(product.isFeatured())
				.createdAt(product.getCreatedAt())
				.updatedAt(product.getUpdatedAt())
				.build();
	}

	public static ProductSummary toProductSummary(ProductReadModel product) {
		return ProductSummary.builder()
				.id(product.getId())
				.name(product.getName())
				.sku(product.getSku())
				.categoryIds(product.getCategoryIds())
				.vendorId(product.getVendorId())
				.vendorName(product.getVendorName())
				.regularPrice(product.getPrice().getRegular())
				.discountedPrice(product.getPrice().getDiscounted())
				.currency(product.getPrice().getCurrency())
				.hasDiscount(product.getPrice().hasDiscount())
				.discountPercentage(product.getPrice().getDiscountPercentage())
				.inStock(product.getStock().isInStock())
				.mainImage(product.getImages() != null && !product.getImages().isEmpty() ?
						product.getImages().get(0) : null)
				.brandName(product.getBrandName())
				.hasVariants(!product.getVariants().isEmpty())
				.build();
	}
}