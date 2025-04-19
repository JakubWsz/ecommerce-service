package pl.ecommerce.customer.write.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.customer.write.api.dto.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Customers", description = "Write operations for customer management")
@RequestMapping("/api/v1/customers")
public interface CustomerApi {

	@Operation(summary = "Register a new customer", description = "Creates a new customer based on the provided request data")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	Mono<CustomerRegistrationResponse> registerCustomer(
			@RequestBody @Valid CustomerRegistrationRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Update customer", description = "Updates the details of an existing customer")
	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> updateCustomer(
			@PathVariable UUID id,
			@RequestBody @Valid CustomerUpdateRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Change customer email", description = "Changes the email address of a customer")
	@PutMapping("/{id}/email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> changeEmail(
			@PathVariable UUID id,
			@RequestParam @NotBlank @Email @Schema(description = "New email address") String newEmail,
			ServerWebExchange exchange);

	@Operation(summary = "Verify customer email", description = "Verifies a customer's email address")
	@PostMapping("/{id}/email/verify")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> verifyEmail(
			@PathVariable UUID id,
			@RequestParam @NotBlank @Schema(description = "Verification token") String token,
			ServerWebExchange exchange);

	@Operation(summary = "Delete customer", description = "Hard deletes a customer account")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> deleteCustomer(
			@PathVariable UUID id,
			ServerWebExchange exchange);

	@Operation(summary = "Verify customer phone number", description = "Verifies a customer's phone number")
	@PostMapping("/{id}/phone/verify")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> verifyPhoneNumber(
			@PathVariable UUID id,
			@RequestParam @NotBlank @Schema(description = "Verification token") String verificationToken,
			ServerWebExchange exchange);

	@Operation(summary = "Add shipping address", description = "Adds a new shipping address for a customer")
	@PostMapping("/{id}/addresses")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> addShippingAddress(
			@PathVariable UUID id,
			@RequestBody @Valid AddShippingAddressRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Update shipping address", description = "Updates a customer's shipping address")
	@PutMapping("/{id}/addresses/{addressId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> updateShippingAddress(
			@PathVariable UUID id,
			@PathVariable UUID addressId,
			@RequestBody @Valid UpdateShippingAddressRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Remove shipping address", description = "Removes a shipping address from a customer")
	@DeleteMapping("/{id}/addresses/{addressId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> removeShippingAddress(
			@PathVariable UUID id,
			@PathVariable UUID addressId,
			ServerWebExchange exchange);

	@Operation(summary = "Update customer preferences", description = "Updates a customer's preferences")
	@PutMapping("/{id}/preferences")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> updatePreferences(
			@PathVariable UUID id,
			@RequestBody @Valid UpdatePreferencesRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Deactivate customer", description = "Deactivates a customer's account")
	@PostMapping("/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> deactivate(
			@PathVariable UUID id,
			@RequestParam @NotBlank @Schema(description = "Reason for deactivation") String reason,
			ServerWebExchange exchange);

	@Operation(summary = "Reactivate customer", description = "Reactivates a customer's account")
	@PostMapping("/{id}/reactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Mono<Void> reactivate(
			@PathVariable UUID id,
			@RequestParam @NotBlank @Schema(description = "Note regarding reactivation") String note,
			ServerWebExchange exchange);
}