package pl.ecommerce.vendor.write.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.vendor.write.api.dto.request.*;
import pl.ecommerce.vendor.write.api.dto.response.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Vendors", description = "Write operations for vendor management")
@RequestMapping("/api/v1/vendors")
public interface VendorApi {

	@Operation(summary = "Register a new vendor", description = "Creates a new vendor based on the provided request data")
	@PostMapping
	Mono<ResponseEntity<VendorRegistrationResponse>> registerVendor(
			@RequestBody @Valid VendorRegistrationRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Update vendor", description = "Updates the details of an existing vendor")
	@PutMapping("/{id}")
	Mono<ResponseEntity<VendorUpdateResponse>> updateVendor(
			@PathVariable UUID id,
			@RequestBody @Valid VendorUpdateRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Add category to vendor", description = "Assigns a product category to the vendor")
	@PostMapping("/{id}/categories")
	Mono<ResponseEntity<CategoryAssignmentResponse>> addCategory(
			@PathVariable UUID id,
			@RequestBody @Valid AddCategoryRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Remove category from vendor", description = "Removes a product category from the vendor")
	@DeleteMapping("/{id}/categories/{categoryId}")
	Mono<ResponseEntity<Void>> removeCategory(
			@PathVariable UUID id,
			@PathVariable UUID categoryId,
			ServerWebExchange exchange);

	@Operation(summary = "Change vendor status", description = "Updates the status of a vendor (active, suspended, etc.)")
	@PutMapping("/{id}/status")
	Mono<ResponseEntity<VendorUpdateResponse>> changeVendorStatus(
			@PathVariable UUID id,
			@RequestBody @Valid VendorStatusChangeRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Verify vendor", description = "Completes the verification process for a vendor")
	@PostMapping("/{id}/verify")
	Mono<ResponseEntity<VendorVerificationResponse>> verifyVendor(
			@PathVariable UUID id,
			@RequestBody @Valid VendorVerificationRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Update bank details", description = "Updates the bank account details for a vendor")
	@PutMapping("/{id}/bank-details")
	Mono<ResponseEntity<BankDetailsResponse>> updateBankDetails(
			@PathVariable UUID id,
			@RequestBody @Valid UpdateBankDetailsRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Delete vendor", description = "Deletes a vendor account")
	@DeleteMapping("/{id}")
	Mono<ResponseEntity<Void>> deleteVendor(
			@PathVariable UUID id,
			ServerWebExchange exchange);
}