package pl.ecommerce.customer.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.customer.api.CustomerMapper;
import pl.ecommerce.customer.api.dto.*;
import pl.ecommerce.customer.aplication.dto.CustomerResponseDto;
import pl.ecommerce.customer.aplication.dto.CustomerSummaryDto;
import pl.ecommerce.customer.aplication.service.CustomerApplicationService;
import pl.ecommerce.customer.aplication.service.CustomerQueryService;
import pl.ecommerce.customer.domain.commands.*;
import pl.ecommerce.customer.domain.valueobjects.AddressType;
import reactor.core.publisher.Mono;


import java.util.UUID;

@Tag(name = "Customers", description = "Endpoints for customer management")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

	private final CustomerApplicationService customerApplicationService;
	private final CustomerQueryService customerQueryService;

	@Operation(summary = "Register a new customer", description = "Creates a new customer based on the provided request data")
	@PostMapping
	public Mono<ResponseEntity<CustomerResponseDto>> registerCustomer(
			@RequestBody CustomerRegistrationRequest request,
			@RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {

		log.info("Received request to register customer with email: {}", request.email());

		// Konwersja z DTO żądania na Command
		RegisterCustomerCommand command = RegisterCustomerCommand.builder()
				.customerId(UUID.randomUUID())
				.email(request.email())
				.firstName(request.firstName())
				.lastName(request.lastName())
				.phoneNumber(request.phoneNumber())
				.password(request.password())
				.consents(request.consents())
				.build();

		return customerApplicationService.registerCustomer(command, ipAddress)
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Get customer by ID", description = "Returns a customer by its ID")
	@GetMapping("/{id}")
	public Mono<ResponseEntity<CustomerResponseDto>> getCustomerById(@PathVariable UUID id) {
		log.info("Received request to get customer with id: {}", id);

		return customerQueryService.findById(id)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Get customers", description = "Returns all active customers")
	@GetMapping
	public Mono<ResponseEntity<Page<CustomerSummaryDto>>> getAllCustomers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "lastName") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		log.info("Received request to get all customers. page={}, size={}", page, size);

		Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

		return customerQueryService.findAllActive(pageRequest)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Update customer", description = "Updates the details of an existing customer")
	@PutMapping("/{id}")
	public Mono<ResponseEntity<CustomerResponseDto>> updateCustomer(
			@PathVariable UUID id,
			@RequestBody CustomerUpdateRequest request) {

		log.info("Received request to update customer with id: {}", id);

		// Konwersja z DTO żądania na Command
		UpdateCustomerCommand command = UpdateCustomerCommand.builder()
				.customerId(id)
				.firstName(request.firstName())
				.lastName(request.lastName())
				.phoneNumber(request.phoneNumber())
				.build();

		return customerApplicationService.updateCustomer(id, command)
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Add address to customer", description = "Adds a new address to a customer")
	@PostMapping("/{id}/addresses")
	public Mono<ResponseEntity<CustomerResponseDto>> addAddress(
			@PathVariable UUID id,
			@RequestBody AddressRequest request) {

		log.info("Received request to add address for customer with id: {}", id);

		// Konwersja z DTO żądania na Command
		AddShippingAddressCommand command = AddShippingAddressCommand.builder()
				.customerId(id)
				.addressId(UUID.randomUUID())
				.street(request.street())
				.city(request.city())
				.postalCode(request.postalCode())
				.country(request.country())
				.state(request.state())
				.addressType(AddressType.valueOf(request.addressType()))
				.isDefault(request.isDefault())
				.build();

		return customerApplicationService.addAddress(id, command)
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Remove address from customer", description = "Removes an address from a customer")
	@DeleteMapping("/{customerId}/addresses/{addressId}")
	public Mono<ResponseEntity<CustomerResponseDto>> removeAddress(
			@PathVariable UUID customerId,
			@PathVariable UUID addressId) {

		log.info("Received request to remove address {} for customer with id: {}", addressId, customerId);

		return customerApplicationService.removeAddress(customerId, addressId)
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Change customer email", description = "Changes the email address of a customer")
	@PutMapping("/{id}/email")
	public Mono<ResponseEntity<CustomerResponseDto>> changeEmail(
			@PathVariable UUID id,
			@RequestBody EmailChangeRequest request) {

		log.info("Received request to change email for customer with id: {}", id);

		// Konwersja z DTO żądania na Command
		ChangeCustomerEmailCommand command = ChangeCustomerEmailCommand.builder()
				.customerId(id)
				.newEmail(request.newEmail())
				.build();

		return customerApplicationService.changeEmail(id, command)
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Verify customer email", description = "Verifies a customer's email address")
	@PostMapping("/{id}/email/verify")
	public Mono<ResponseEntity<CustomerResponseDto>> verifyEmail(
			@PathVariable UUID id,
			@RequestBody VerificationRequest request) {

		log.info("Received request to verify email for customer with id: {}", id);

		return customerApplicationService.verifyEmail(id, request.token())
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Update customer preferences", description = "Updates a customer's preferences")
	@PutMapping("/{id}/preferences")
	public Mono<ResponseEntity<CustomerResponseDto>> updatePreferences(
			@PathVariable UUID id,
			@RequestBody PreferencesRequest request) {

		log.info("Received request to update preferences for customer with id: {}", id);

		// Konwersja z DTO żądania na Command
		UpdateCustomerPreferencesCommand command = UpdateCustomerPreferencesCommand.builder()
				.customerId(id)
				.preferences(CustomerMapper.toPreferencesDomain(request))
				.build();

		return customerApplicationService.updatePreferences(id, command)
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Deactivate customer", description = "Deactivates a customer account")
	@PutMapping("/{id}/deactivate")
	public Mono<ResponseEntity<CustomerResponseDto>> deactivateCustomer(
			@PathVariable UUID id,
			@RequestBody(required = false) DeactivateRequest request) {

		log.info("Received request to deactivate customer with id: {}", id);

		String reason = request != null ? request.reason() : "Customer requested deactivation";

		return customerApplicationService.deactivateCustomer(id, reason)
				.flatMap(customerQueryService::findById)
				.map(CustomerMapper::toResponseDto)
				.map(ResponseEntity::ok);
	}

	@Operation(summary = "Delete customer", description = "Hard deletes a customer account")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> deleteCustomer(@PathVariable UUID id) {
		log.info("Received request to delete customer with id: {}", id);

		return customerApplicationService.deleteCustomer(id)
				.then();
	}
}