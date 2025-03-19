package pl.ecommerce.product.read.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.product.read.api.dto.StockVerificationRequest;
import pl.ecommerce.product.read.api.dto.StockVerificationResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Stock Verification", description = "API for verifying product stock before purchase")
@RequestMapping("/api/v1/products/stock")
public interface StockVerificationApi {

	@Operation(summary = "Verify stock availability",
			description = "Verifies if products are available in requested quantities before purchase")
	@PostMapping("/verify")
	Mono<ResponseEntity<StockVerificationResponse>> verifyStock(
			@RequestBody StockVerificationRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Check product availability",
			description = "Checks if a specific product is available in requested quantity")
	@GetMapping("/{productId}/available")
	Mono<ResponseEntity<Boolean>> isProductAvailable(
			@PathVariable UUID productId,
			@RequestParam(defaultValue = "1") int quantity,
			ServerWebExchange exchange);

	@Operation(summary = "Verify multiple products",
			description = "Batch verification of multiple products availability")
	@PostMapping("/verify-batch")
	Mono<ResponseEntity<StockVerificationResponse>> verifyMultipleProducts(
			@RequestBody StockVerificationRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Reserve product stock",
			description = "Creates a temporary reservation for product stock")
	@PostMapping("/{productId}/reserve")
	Mono<ResponseEntity<Boolean>> reserveStock(
			@PathVariable UUID productId,
			@RequestParam @Parameter(description = "Quantity to reserve") int quantity,
			@RequestParam @Parameter(description = "Unique reservation identifier") String reservationId,
			ServerWebExchange exchange);

	@Operation(summary = "Release reservation",
			description = "Releases a previously made stock reservation")
	@PostMapping("/{productId}/reservations/{reservationId}/release")
	Mono<ResponseEntity<Boolean>> releaseReservation(
			@PathVariable UUID productId,
			@PathVariable String reservationId,
			ServerWebExchange exchange);
}