package pl.ecommerce.customer.api.dto;

import java.util.List;

public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements
) {}