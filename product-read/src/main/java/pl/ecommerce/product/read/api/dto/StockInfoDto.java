package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO reprezentujące informacje o stanie magazynowym produktu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoDto {
	private int available;
	private int reserved;
	private boolean inStock;
	private boolean lowStock;

	/**
	 * Zwraca liczbę dostępnych sztuk z uwzględnieniem rezerwacji
	 */
	public int getAvailableForPurchase() {
		return Math.max(0, available - reserved);
	}

	/**
	 * Sprawdza, czy produkt jest dostępny do zakupu w określonej ilości
	 */
	public boolean isAvailableForQuantity(int quantity) {
		return getAvailableForPurchase() >= quantity;
	}

	/**
	 * Zwraca tekst dostępności do wyświetlenia
	 */
	public String getAvailabilityText() {
		if (!inStock) {
			return "Niedostępny";
		}

		if (lowStock) {
			return "Ostatnie sztuki";
		}

		return "Dostępny";
	}

	/**
	 * Zwraca klasę CSS dla statusu dostępności
	 */
	public String getAvailabilityClass() {
		if (!inStock) {
			return "out-of-stock";
		}

		if (lowStock) {
			return "low-stock";
		}

		return "in-stock";
	}
}