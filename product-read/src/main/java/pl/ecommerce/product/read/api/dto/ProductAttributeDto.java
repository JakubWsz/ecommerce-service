package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeDto {
	private String name;
	private String value;
	private String unit;

	/**
	 * Zwraca sformatowaną wartość atrybutu
	 */
	public String getFormattedValue() {
		if (unit == null || unit.isEmpty()) {
			return value;
		}
		return value + " " + unit;
	}

	@Override
	public String toString() {
		return getFormattedValue();
	}
}
