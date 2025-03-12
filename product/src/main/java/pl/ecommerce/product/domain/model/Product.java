package pl.ecommerce.product.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.javamoney.moneta.Money;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Objects.isNull;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotBlank(message = "Product name is required")
	@Setter
	private String name;

	@Column(length = 4000)
	@Setter
	private String description;

	@NotNull(message = "Price is required")
	@Min(value = 0, message = "Price cannot be negative")
	private BigDecimal price;

	@NotBlank(message = "Currency is required")
	private String currency;

	@NotNull(message = "Stock is required")
	@Min(value = 0, message = "Stock cannot be negative")
	@Setter
	private Integer stock;

	@Column(name = "reserved_stock")
	@Builder.Default
	@Setter
	private Integer reservedStock = 0;

	@NotBlank(message = "Vendor ID is required")
	@Column(name = "vendor_id")
	private UUID vendorId;

	@Builder.Default
	@Setter
	private boolean active = true;

	@Version
	private Long version;

	@Column(name = "created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(
			name = "product_categories",
			joinColumns = @JoinColumn(name = "product_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id")
	)
	@Builder.Default
	private Set<Category> categories = new HashSet<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ProductImage> images = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
	@Builder.Default
	private List<ProductReservation> reservations = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "product_attributes", joinColumns = @JoinColumn(name = "product_id"))
	@MapKeyColumn(name = "attribute_key")
	@Column(name = "attribute_value")
	private Map<String, String> attributes = new HashMap<>();

	public static Product create(String name, String description, MonetaryAmount price, Integer stock,
								 UUID vendorId, Map<String, String> attributes) {
		if (isNull(price)) {
			throw new IllegalArgumentException("Price cannot be null");
		}

		return Product.builder()
				.name(name)
				.description(description)
				.price(price.getNumber().numberValue(BigDecimal.class))
				.currency(price.getCurrency().getCurrencyCode())
				.stock(stock)
				.vendorId(vendorId)
				.reservedStock(0)
				.attributes(attributes)
				.build();
	}

	public MonetaryAmount getPrice() {
		if (price == null || currency == null) {
			return null;
		}
		return Money.of(price, currency);
	}

	public void setPrice(MonetaryAmount monetaryAmount) {
		if (isNull(monetaryAmount)) {
			this.price = null;
			this.currency = null;
		} else {
			this.price = monetaryAmount.getNumber().numberValue(BigDecimal.class);
			this.currency = monetaryAmount.getCurrency().getCurrencyCode();
		}
	}

	public int getAvailableStock() {
		return stock - reservedStock;
	}

	public boolean canReserve(int quantity) {
		return getAvailableStock() >= quantity;
	}

	public void reserve(int quantity) {
		if (!canReserve(quantity)) {
			throw new IllegalStateException("Not enough stock available to reserve");
		}
		reservedStock += quantity;
	}

	public void confirmReservation(int quantity) {
		if (reservedStock < quantity) {
			throw new IllegalStateException("Not enough reserved stock to confirm");
		}
		reservedStock -= quantity;
		stock -= quantity;
	}

	public void cancelReservation(int quantity) {
		if (reservedStock < quantity) {
			throw new IllegalStateException("Not enough reserved stock to cancel");
		}
		reservedStock -= quantity;
	}

	public void addCategory(Category category) {
		categories.add(category);
		category.getProducts().add(this);
	}

	public void removeCategory(Category category) {
		categories.remove(category);
		category.getProducts().remove(this);
	}

	public void addImage(ProductImage image) {
		images.add(image);
		image.setProduct(this);
	}

	public void removeImage(ProductImage image) {
		images.remove(image);
		image.setProduct(null);
	}
}