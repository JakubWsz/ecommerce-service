package pl.ecommerce.vendor.read.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.vendor.read.api.dto.VendorResponse;
import pl.ecommerce.vendor.read.api.dto.VendorSummary;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Vendors", description = "Read operations for vendor data")
@RequestMapping("/api/v1/vendors")
public interface VendorQueryApi {

	@Operation(summary = "Get vendor by ID", description = "Returns a vendor by its ID")
	@GetMapping("/{id}")
	Mono<ResponseEntity<VendorResponse>> getVendorById(
			@PathVariable UUID id,
			ServerWebExchange exchange);

	@Operation(summary = "Get vendor by email", description = "Returns a vendor by email address")
	@GetMapping("/email/{email}")
	Mono<ResponseEntity<VendorResponse>> getVendorByEmail(
			@PathVariable String email,
			ServerWebExchange exchange);

	@Operation(summary = "Get vendors by status", description = "Returns vendors by status")
	@GetMapping("/status/{status}")
	Mono<ResponseEntity<Page<VendorSummary>>> getVendorsByStatus(
			@PathVariable String status,
			ServerWebExchange exchange,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir);

	@Operation(summary = "Get vendors", description = "Returns all active vendors with pagination")
	@GetMapping
	Mono<ResponseEntity<Page<VendorSummary>>> getAllVendors(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Search vendors", description = "Searches vendors by name or business name")
	@GetMapping("/search")
	Mono<ResponseEntity<Page<VendorSummary>>> searchVendors(
			@RequestParam String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			ServerWebExchange exchange);

	@Operation(summary = "Get vendors by category", description = "Returns vendors that sell in a specific category")
	@GetMapping("/category/{categoryId}")
	Mono<ResponseEntity<Page<VendorSummary>>> getVendorsByCategory(
			@PathVariable UUID categoryId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			ServerWebExchange exchange);
}