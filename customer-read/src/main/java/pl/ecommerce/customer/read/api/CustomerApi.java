package pl.ecommerce.customer.read.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.customer.read.aplication.dto.CustomerResponse;
import pl.ecommerce.customer.read.aplication.dto.CustomerSummary;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Customers", description = "Read operations for customer data")
@RequestMapping("/api/v1/customers")
public interface CustomerApi {

	@Operation(summary = "Get customer by ID", description = "Returns a customer by its ID")
	@GetMapping("/{id}")
	Mono<ResponseEntity<CustomerResponse>> getCustomerById(
			@PathVariable UUID id,
			ServerWebExchange exchange);

	@Operation(summary = "Get customer by email", description = "Returns a customer by email address")
	@GetMapping("/email/{email}")
	Mono<ResponseEntity<CustomerResponse>> getCustomerByEmail(
			@PathVariable String email,
			ServerWebExchange exchange);

	@Operation(summary = "Get customers by status", description = "Returns a customers by status")
	@GetMapping("/status/{status}")
	Mono<ResponseEntity<Page<CustomerSummary>>> getCustomerByStatus(
			@PathVariable String status,
			ServerWebExchange exchange,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "lastName") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir);

	@Operation(summary = "Get customers", description = "Returns all active customers with pagination")
	@GetMapping
	Mono<ResponseEntity<Page<CustomerSummary>>> getAllCustomers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "lastName") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			ServerWebExchange exchange);

	@Operation(summary = "Search customers", description = "Searches customers by name")
	@GetMapping("/search")
	Mono<ResponseEntity<Page<CustomerSummary>>> searchCustomers(
			@RequestParam String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			ServerWebExchange exchange);
}