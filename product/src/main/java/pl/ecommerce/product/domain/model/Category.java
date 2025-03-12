package pl.ecommerce.product.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotBlank(message = "Category name is required")
	@Column(unique = true)
	@Setter
	private String name;

	@Setter
	private String description;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
	@Builder.Default
	private List<Category> children = new ArrayList<>();

	@Builder.Default
	@Setter
	private boolean active = true;

	@Column(name = "created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
	@Builder.Default
	private Set<Product> products = new HashSet<>();

	@Version
	private Long version;

	public void addChild(Category child) {
		children.add(child);
		child.setParent(this);
	}

	public void removeChild(Category child) {
		children.remove(child);
		child.setParent(null);
	}
}

