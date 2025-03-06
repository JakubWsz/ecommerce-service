package pl.ecommerce.vendor.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class Category {
	UUID id;
	String name;
	String description;
}
