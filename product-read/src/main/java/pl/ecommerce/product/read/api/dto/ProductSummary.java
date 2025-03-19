package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * DTO reprezentujące skrócone informacje o produkcie (do listy produktów)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummary {
	private UUID id;
	private String name;
	private String sku;
	private Set<UUID> categoryIds;
	private UUID vendorId;
	private String vendorName;
	private BigDecimal regularPrice;
	private BigDecimal discountedPrice;
	private String currency;
	private boolean hasDiscount;
	private int discountPercentage;
	private boolean inStock;
	private String mainImage;
	private String brandName;
	private boolean hasVariants;
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
	 * Oblicza kwotę zniżki
	 */
	public BigDecimal getDiscountAmount() {
		if (!hasDiscount || regularPrice == null || discountedPrice == null) {
			return BigDecimal.ZERO;
		}
		return regularPrice.subtract(discountedPrice);
	}

	/**
	 * Buduje podstawowy URL produktu
	 */
	public String getProductUrl() {
		return "/products/" + id;
	}

	/**
	 * Pobiera pierwszy obrazek produktu lub placeholder, jeśli brak obrazka
	 */
	public String getImageUrl() {
		if (mainImage == null || mainImage.isEmpty()) {
			return "/images/placeholder-product.png";
		}
		return mainImage;
	}

	/**
	 * Zwraca tekst zniżki do wyświetlenia
	 */
	public String getDiscountText() {
		if (!hasDiscount) {
			return "";
		}
		return "-" + discountPercentage + "%";
	}
}