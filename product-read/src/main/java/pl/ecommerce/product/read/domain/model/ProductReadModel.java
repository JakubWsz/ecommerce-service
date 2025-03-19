package pl.ecommerce.product.read.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class ProductReadModel {
	@Id
	private UUID id;

	private String name;

	private String description;

	@Indexed(unique = true)
	private String sku;

	private Set<UUID> categoryIds = new HashSet<>();

	private List<String> categoryNames = new ArrayList<>();

	private UUID vendorId;

	private String vendorName;

	private PriceInfo price;

	private List<ProductAttribute> attributes = new ArrayList<>();

	private List<ProductVariant> variants = new ArrayList<>();

	private StockInfo stock;

	private String status;

	private List<String> images = new ArrayList<>();

	private String brandName;

	private boolean featured;

	private Map<String, String> metadata = new HashMap<>();

	private Instant createdAt;

	private Instant updatedAt;

	private String lastTraceId;

	private String lastSpanId;

	private String lastOperation;

	private Instant lastUpdatedAt;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PriceInfo {
		private BigDecimal regular;
		private BigDecimal discounted;
		private String currency;

		public BigDecimal getCurrentPrice() {
			return discounted != null ? discounted : regular;
		}

		public boolean hasDiscount() {
			return discounted != null && discounted.compareTo(regular) < 0;
		}

		public BigDecimal getDiscountAmount() {
			if (!hasDiscount()) {
				return BigDecimal.ZERO;
			}
			return regular.subtract(discounted);
		}

		public int getDiscountPercentage() {
			if (!hasDiscount() || regular.compareTo(BigDecimal.ZERO) <= 0) {
				return 0;
			}
			return regular.subtract(discounted).multiply(new BigDecimal(100)).divide(regular, 0, BigDecimal.ROUND_HALF_UP).intValue();
		}
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StockInfo {
		private int available;
		private int reserved;
		private String warehouseId;

		public boolean isInStock() {
			return available > 0;
		}

		public boolean isLowStock() {
			return available > 0 && available <= 5; // Example threshold
		}
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ProductAttribute {
		private String name;
		private String value;
		private String unit;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ProductVariant {
		private UUID id;
		private String sku;
		private List<ProductAttribute> attributes;
		private PriceInfo price;
		private StockInfo stock;
	}
}