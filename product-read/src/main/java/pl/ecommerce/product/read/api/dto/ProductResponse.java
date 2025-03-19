package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTO reprezentujące pełne informacje o produkcie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
	private UUID id;
	private String name;
	private String description;
	private String sku;
	private Set<UUID> categories;
	private List<String> categoryNames;
	private UUID vendorId;
	private String vendorName;
	private BigDecimal regularPrice;
	private BigDecimal discountedPrice;
	private String currency;
	private List<ProductAttributeDto> attributes;
	private List<ProductVariantDto> variants;
	private StockInfoDto stock;
	private String status;
	private List<String> images;
	private String brandName;
	private boolean featured;
	private Instant createdAt;
	private Instant updatedAt;
	private String traceId;

	/**
	 * Zwraca aktualną cenę produktu (promocyjną, jeśli dostępna)
	 */
	public BigDecimal getCurrentPrice() {
		if (discountedPrice != null) {
			return discountedPrice;
		}
		return regularPrice;
	}

	/**
	 * Sprawdza, czy produkt ma zniżkę
	 */
	public boolean hasDiscount() {
		return discountedPrice != null && regularPrice.compareTo(discountedPrice) > 0;
	}

	/**
	 * Oblicza kwotę zniżki
	 */
	public BigDecimal getDiscountAmount() {
		if (!hasDiscount()) {
			return BigDecimal.ZERO;
		}
		return regularPrice.subtract(discountedPrice);
	}

	/**
	 * Oblicza procent zniżki
	 */
	public int getDiscountPercentage() {
		if (!hasDiscount() || regularPrice.compareTo(BigDecimal.ZERO) <= 0) {
			return 0;
		}
		return regularPrice.subtract(discountedPrice)
				.multiply(new BigDecimal(100))
				.divide(regularPrice, 0, BigDecimal.ROUND_HALF_UP)
				.intValue();
	}

	/**
	 * Sprawdza, czy produkt ma warianty
	 */
	public boolean hasVariants() {
		return variants != null && !variants.isEmpty();
	}

	/**
	 * Sprawdza, czy produkt jest aktywny
	 */
	public boolean isActive() {
		return "ACTIVE".equals(status);
	}
}