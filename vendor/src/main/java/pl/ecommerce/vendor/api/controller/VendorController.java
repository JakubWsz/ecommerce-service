package pl.ecommerce.vendor.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.vendor.api.dto.VendorRequest;
import pl.ecommerce.vendor.api.dto.VendorResponse;
import pl.ecommerce.vendor.api.dto.VendorUpdateRequest;
import pl.ecommerce.vendor.api.mapper.VendorMapper;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.service.VendorService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Vendors", description = "Endpoints for vendor management")
@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Slf4j
public class VendorController {

	private final VendorService vendorService;

	@Operation(summary = "Register a new vendor", description = "Creates a new vendor in the system")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<VendorResponse> registerVendor(@RequestBody VendorRequest request) {
		Vendor vendor = VendorMapper.toVendor(request);
		return vendorService.registerVendor(vendor)
				.map(VendorMapper::toResponse);
	}

	@Operation(summary = "Get vendor by ID", description = "Returns a vendor by its ID")
	@GetMapping("/{id}")
	public Mono<VendorResponse> getVendorById(@PathVariable String id) {
		return vendorService.getVendorById(UUID.fromString(id))
				.map(VendorMapper::toResponse);
	}

	@Operation(summary = "Get all vendors", description = "Returns all active vendors")
	@GetMapping
	public Flux<VendorResponse> getAllVendors() {
		return vendorService.getAllVendors()
				.map(VendorMapper::toResponse);
	}

	@Operation(summary = "Update vendor", description = "Updates the details of an existing vendor")
	@PutMapping("/{id}")
	public Mono<VendorResponse> updateVendor(
			@PathVariable String id,
			@RequestBody VendorUpdateRequest request) {
		Vendor vendorUpdate = VendorMapper.toVendor(request);
		return vendorService.updateVendor(UUID.fromString(id), vendorUpdate)
				.map(VendorMapper::toResponse);
	}

	@Operation(summary = "Update vendor status", description = "Updates the status of a vendor")
	@PutMapping("/{id}/status")
	public Mono<VendorResponse> updateVendorStatus(
			@PathVariable String id,
			@RequestParam String status,
			@RequestParam(required = false) String reason) {
		return vendorService.updateVendorStatus(UUID.fromString(id), Vendor.VendorStatus.valueOf(status), reason)
				.map(VendorMapper::toResponse);
	}

	@Operation(summary = "Update verification status", description = "Updates the verification status of a vendor")
	@PutMapping("/{id}/verification")
	public Mono<VendorResponse> updateVerificationStatus(
			@PathVariable String id,
			@RequestParam String status) {
		return vendorService.updateVerificationStatus(UUID.fromString(id), Vendor.VerificationStatus.valueOf(status))
				.map(VendorMapper::toResponse);
	}

	@Operation(summary = "Deactivate vendor", description = "Deactivates a vendor (soft delete)")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> deactivateVendor(@PathVariable String id) {
		return vendorService.deactivateVendor(UUID.fromString(id));
	}
}