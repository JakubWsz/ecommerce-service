package pl.ecommerce.product.read.application.mapper;

import lombok.experimental.UtilityClass;
import pl.ecommerce.product.read.api.dto.*;
import pl.ecommerce.product.read.domain.model.ProductReadModel;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@UtilityClass
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
				.attributes(mapAttributes(product.getAttributes()))
				.variants(mapVariants(product.getVariants()))
				.stock(mapStockInfo(product.getStock()))
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
				.mainImage(getFirstImage(product.getImages()))
				.brandName(product.getBrandName())
				.hasVariants(nonNull(product.getVariants()) && !product.getVariants().isEmpty())
				.build();
	}

	private static List<ProductAttributeDto> mapAttributes(List<ProductReadModel.ProductAttribute> attributes) {
		return Optional.ofNullable(attributes)
				.orElse(Collections.emptyList())
				.stream()
				.map(attr -> new ProductAttributeDto(attr.getName(), attr.getValue(), attr.getUnit()))
				.collect(Collectors.toList());
	}

	private static List<ProductVariantDto> mapVariants(List<ProductReadModel.ProductVariant> variants) {
		return Optional.ofNullable(variants)
				.orElse(Collections.emptyList())
				.stream()
				.map(variant -> ProductVariantDto.builder()
						.id(variant.getId())
						.sku(variant.getSku())
						.attributes(mapAttributes(variant.getAttributes()))
						.regularPrice(variant.getPrice().getRegular())
						.discountedPrice(variant.getPrice().getDiscounted())
						.currency(variant.getPrice().getCurrency())
						.stock(mapStockInfo(variant.getStock()))
						.build())
				.collect(Collectors.toList());
	}

	private static StockInfoDto mapStockInfo(ProductReadModel.StockInfo stock) {
		return new StockInfoDto(
				stock.getAvailable(),
				stock.getReserved(),
				stock.isInStock(),
				stock.isLowStock());
	}

	private static String getFirstImage(List<String> images) {
		return (nonNull(images) && !images.isEmpty()) ? images.getFirst() : null;
	}
}
