package pl.ecommerce.customer.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.customer.api.dto.*;
import pl.ecommerce.customer.domain.model.Address;
import pl.ecommerce.customer.domain.model.Customer;
import pl.ecommerce.customer.domain.model.GeoLocationData;
import pl.ecommerce.customer.domain.model.PersonalData;
import pl.ecommerce.customer.domain.model.AddressType;
import pl.ecommerce.customer.domain.service.CustomerService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Customers", description = "Endpoints for customer management")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

	private final CustomerService customerService;

	@Operation(summary = "Register a new customer", description = "Creates a new customer based on the provided request data")
	@PostMapping
	public Mono<CustomerResponse> registerCustomer(@RequestBody CreateCustomerRequest request,
												   @RequestParam(required = false) String ipAddress) {
		Customer customer = mapToCustomer(request);
		return customerService.registerCustomer(customer, ipAddress)
				.map(this::toResponse);
	}

	@Operation(summary = "Get customer by ID", description = "Returns a customer by its ID")
	@GetMapping("/{id}")
	public Mono<CustomerResponse> getCustomerById(@PathVariable UUID id) {
		return customerService.getCustomerById(id)
				.map(this::toResponse);
	}

	@Operation(summary = "Get customers", description = "Returns all customers")

	@GetMapping
	public Flux<CustomerResponse> getAllCustomers() {
		return customerService.getAllCustomers()
				.map(this::toResponse);
	}

	@Operation(summary = "Update customer", description = "Updates the details of an existing customer")
	@PutMapping("/{id}")
	public Mono<CustomerResponse> updateCustomer(@PathVariable UUID id, @RequestBody UpdateCustomerRequest request) {
		Customer customerUpdate = mapToCustomer(request);
		return customerService.updateCustomer(id, customerUpdate)
				.map(this::toResponse);
	}

	@Operation(summary = "Delete customer", description = "Deactivates a customer by ID")
	@DeleteMapping("/{id}")
	public Mono<Void> deleteCustomer(@PathVariable UUID id) {
		return customerService.deleteCustomer(id);
	}

	private CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.isActive(),
				customer.getRegistrationIp(),
				customer.getCreatedAt().toString(),
				customer.getUpdatedAt().toString(),
				customer.getPersonalData() != null ? toPersonalDataDto(customer.getPersonalData()) : null,
				customer.getAddresses() != null
						? customer.getAddresses().stream().map(this::toAddressDto).toList()
						: List.of(),
				customer.getGeoLocationData() != null ? toGeoLocationDataDto(customer.getGeoLocationData()) : null
		);
	}

	private PersonalDataDto toPersonalDataDto(PersonalData pd) {
		return new PersonalDataDto(pd.getEmail(), pd.getFirstName(), pd.getLastName(), pd.getPhoneNumber());
	}

	private AddressDto toAddressDto(Address addr) {
		return new AddressDto(
				addr.getStreet(),
				addr.getBuildingNumber(),
				addr.getApartmentNumber(),
				addr.getCity(),
				addr.getState(),
				addr.getPostalCode(),
				addr.getCountry(),
				addr.isDefault(),
				addr.getAddressType().name()
		);
	}

	private GeoLocationDataDto toGeoLocationDataDto(GeoLocationData geo) {
		return new GeoLocationDataDto(
				geo.getCountry(),
				geo.getCity(),
				geo.getVoivodeship(),
				geo.getPostalCode()
		);
	}

	private Customer mapToCustomer(CreateCustomerRequest request) {
		PersonalData pd = new PersonalData(
				request.personalData().email(),
				request.personalData().firstName(),
				request.personalData().lastName(),
				request.personalData().phoneNumber()
		);
		List<Address> addresses = request.addresses().stream()
				.map(addr -> new Address(
						addr.street(),
						addr.buildingNumber(),
						addr.apartmentNumber(),
						addr.city(),
						addr.state(),
						addr.postalCode(),
						addr.country(),
						addr.isDefault(),
						AddressType.valueOf(addr.addressType())
				))
				.toList();

		return new Customer(
				true,
				"",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				request.personalData() != null,
				LocalDateTime.now(),
				false,
				false,
				0,
				pd,
				addresses,
				null,
				List.of()
		);
	}

	private Customer mapToCustomer(UpdateCustomerRequest request) {
		PersonalData pd = new PersonalData(
				request.personalData().email(),
				request.personalData().firstName(),
				request.personalData().lastName(),
				request.personalData().phoneNumber()
		);
		List<Address> addresses = request.addresses().stream()
				.map(addr -> new Address(
						addr.street(),
						addr.buildingNumber(),
						addr.apartmentNumber(),
						addr.city(),
						addr.state(),
						addr.postalCode(),
						addr.country(),
						addr.isDefault(),
						AddressType.valueOf(addr.addressType())
				))
				.toList();
		return new Customer(
				true,
				"",
				LocalDateTime.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				request.personalData() != null,
				LocalDateTime.now(),
				false,
				false,
				0,
				pd,
				addresses,
				null,
				List.of()
		);
	}
}
