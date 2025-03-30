package pl.ecommerce.product.read.infrastructure.cahce;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class CacheKeyGenerator {

	private static final String PRODUCT_KEY_PREFIX = "product:";
	private static final String PRODUCT_SUMMARY_KEY_PREFIX = "product:summary:";
	private static final String PRODUCT_RESPONSE_KEY_PREFIX = "product:response:";
	private static final String PRODUCT_SKU_KEY_PREFIX = "product:sku:";

	private static final String SEARCH_RESULTS_KEY_PREFIX = "search:results:";
	private static final String CATEGORY_PRODUCTS_KEY_PREFIX = "category:products:";
	private static final String VENDOR_PRODUCTS_KEY_PREFIX = "vendor:products:";
	private static final String FEATURED_PRODUCTS_KEY_PREFIX = "featured:products:";
	private static final String FILTERED_PRODUCTS_KEY_PREFIX = "filtered:products:";
	private static final String RELATED_PRODUCTS_KEY_PREFIX = "related:products:";

	public static String productKey(UUID productId) {
		return PRODUCT_KEY_PREFIX + productId;
	}

	public static String productSummaryKey(UUID productId) {
		return PRODUCT_SUMMARY_KEY_PREFIX + productId;
	}

	public static String productResponseKey(UUID productId) {
		return PRODUCT_RESPONSE_KEY_PREFIX + productId;
	}

	public static String skuKey(String sku) {
		return PRODUCT_SKU_KEY_PREFIX + sku;
	}

	public static String searchResultsKey(String query, Pageable pageable) {
		return SEARCH_RESULTS_KEY_PREFIX + query + ":" + createPageableKey(pageable);
	}

	public static String categoryProductsKey(UUID categoryId, Pageable pageable) {
		return CATEGORY_PRODUCTS_KEY_PREFIX + categoryId + ":" + createPageableKey(pageable);
	}

	public static String vendorProductsKey(UUID vendorId, Pageable pageable) {
		return VENDOR_PRODUCTS_KEY_PREFIX + vendorId + ":" + createPageableKey(pageable);
	}

	public static String featuredProductsKey(Pageable pageable) {
		return FEATURED_PRODUCTS_KEY_PREFIX + createPageableKey(pageable);
	}

	public static String filteredProductsKey(Set<UUID> categories, Set<UUID> vendors, UUID brandId,
											 BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, Pageable pageable) {
		StringBuilder keyBuilder = new StringBuilder(FILTERED_PRODUCTS_KEY_PREFIX);
		if (categories != null && !categories.isEmpty()) {
			keyBuilder.append("cat:")
					.append(categories.stream()
							.sorted()
							.map(UUID::toString)
							.collect(Collectors.joining("-")))
					.append(":");
		}
		if (vendors != null && !vendors.isEmpty()) {
			keyBuilder.append("ven:")
					.append(vendors.stream()
							.sorted()
							.map(UUID::toString)
							.collect(Collectors.joining("-")))
					.append(":");
		}
		if (brandId != null) {
			keyBuilder.append("brand:").append(brandId).append(":");
		}
		if (minPrice != null) {
			keyBuilder.append("min:").append(minPrice).append(":");
		}
		if (maxPrice != null) {
			keyBuilder.append("max:").append(maxPrice).append(":");
		}
		if (inStock != null) {
			keyBuilder.append("stock:").append(inStock).append(":");
		}
		keyBuilder.append(createPageableKey(pageable));
		return keyBuilder.toString();
	}

	public static String relatedProductsKey(UUID productId, int limit) {
		return RELATED_PRODUCTS_KEY_PREFIX + productId + ":" + limit;
	}

	private static String createPageableKey(Pageable pageable) {
		if (pageable == null) {
			return "p0s20";
		}
		return "p" + pageable.getPageNumber() +
				"s" + pageable.getPageSize() +
				"sort" + (pageable.getSort().isSorted() ?
				pageable.getSort().toString().replaceAll("[^a-zA-Z0-9]", "") : "none");
	}
}
