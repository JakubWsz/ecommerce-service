package pl.ecommerce.vendor.domain.model;

import lombok.*;

import java.util.UUID;

import org.springframework.data.mongodb.core.mapping.Field;

@ToString
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Category {
	@Field("id")
	private UUID id;

	@Field("name")
	private String name;

	@Field("description")
	private String description;
}