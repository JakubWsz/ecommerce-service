package pl.ecommerce.product.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_images")
public class ProductImage {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@NotBlank(message = "Image URL is required")
	private String url;

	@Column(name = "sort_order")
	@Setter
	private Integer sortOrder = 0;

	@Column(name = "created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Version
	private Long version;

}