package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDto {
	private UUID id;
	private String sku;
	private List<ProductAttributeDto> attributes;
	private BigDecimal regularPrice;
	private BigDecimal discountedPrice;
	private String currency;
	private StockInfoDto stock;

	/**
	 * Zwraca aktualną cenę wariantu (promocyjną, jeśli dostępna)
	 */
	public BigDecimal getCurrentPrice() {
		if (discountedPrice != null) {
			return discountedPrice;
		}
		return regularPrice;
	}

	/**
	 * Sprawdza, czy wariant ma zniżkę
	 */
	public boolean hasDiscount() {
		return discountedPrice != null && regularPrice.compareTo(discountedPrice) > 0;
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
	 * Zwraca czytelną nazwę wariantu
	 */
	public String getVariantName() {
		if (attributes == null || attributes.isEmpty()) {
			return "Standard";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < attributes.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(attributes.get(i).getName()).append(": ")
					.append(attributes.get(i).getFormattedValue());
		}
		return sb.toString();
	}
}