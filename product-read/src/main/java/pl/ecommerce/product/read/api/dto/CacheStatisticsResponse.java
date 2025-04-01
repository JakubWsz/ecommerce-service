package pl.ecommerce.product.read.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatisticsResponse {
	private long hits;
	private long misses;
	private long size;
	private double hitRatio;
	private long memoryUsage;
	private String traceId;
}