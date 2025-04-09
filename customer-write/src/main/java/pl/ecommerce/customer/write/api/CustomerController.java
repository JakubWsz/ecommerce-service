package pl.ecommerce.customer.write.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracedOperation;
import pl.ecommerce.customer.write.api.dto.*;
import pl.ecommerce.customer.write.api.mapper.CommandMapper;
import pl.ecommerce.customer.write.api.mapper.ResponseMapper;
import pl.ecommerce.customer.write.application.CustomerApplicationService;
import pl.ecommerce.customer.write.domain.commands.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerController implements CustomerApi {

	private final CustomerApplicationService customerApplicationService;

	@Override
	@TracedOperation("registerCustomer")
	public Mono<ResponseEntity<CustomerRegistrationResponse>> registerCustomer(
			CustomerRegistrationRequest request, ServerWebExchange exchange) {
		log.debug("Received registerCustomer request for email: {}", request.email());
		var registerCustomerCommand = CommandMapper.map(request);
		return customerApplicationService.registerCustomer(registerCustomerCommand)
				.map(customerId -> {
					log.debug("Customer registered successfully: {}", customerId);
					return ResponseEntity
							.status(HttpStatus.CREATED)
							.body(ResponseMapper.map(customerId, request));
				});
	}

	@Override
	@TracedOperation("updateCustomer")
	public Mono<ResponseEntity<CustomerUpdateResponse>> updateCustomer(
			UUID id, CustomerUpdateRequest request, ServerWebExchange exchange) {
		log.info("Received updateCustomer request for id: {}", id);
		var updateCustomerCommand = CommandMapper.map(id, request);
		return customerApplicationService.updateCustomer(updateCustomerCommand)
				.map(customerId -> ResponseEntity
						.ok()
						.body(ResponseMapper.map(customerId, request)));
	}

	@Override
	@TracedOperation("changeEmail")
	public Mono<ResponseEntity<CustomerEmailChangeResponse>> changeEmail(
			UUID id, String newEmail, ServerWebExchange exchange) {

			var changeCustomerEmailCommand = ChangeCustomerEmailCommand.builder()
					.customerId(id)
					.newEmail(newEmail)
					.build();
			return customerApplicationService.changeEmail(changeCustomerEmailCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerEmailChangeResponse(customerId, newEmail)));
		}


	@Override
	@TracedOperation("verifyEmail")
	public Mono<ResponseEntity<CustomerVerificationResponse>> verifyEmail(
			UUID id, String token, ServerWebExchange exchange) {

			var verifyCustomerEmailCommand = VerifyCustomerEmailCommand.builder()
					.customerId(id)
					.verificationToken(token)
					.build();
			return customerApplicationService.verifyEmail(verifyCustomerEmailCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerVerificationResponse(customerId)));
	}

	@Override
	@TracedOperation("deleteCustomer")
	public Mono<ResponseEntity<Void>> deleteCustomer(UUID id, ServerWebExchange exchange) {
			return customerApplicationService.deleteCustomer(id)
					.then(Mono.just(ResponseEntity.noContent().build()));
	}

	@Override
	@TracedOperation("verifyPhoneNumber")
	public Mono<ResponseEntity<CustomerPhoneVerificationResponse>> verifyPhoneNumber(
			UUID id, String verificationToken, ServerWebExchange exchange) {
			var verifyCustomerPhoneCommand = VerifyCustomerPhoneCommand.builder()
					.customerId(id)
					.verificationToken(verificationToken)
					.build();
			return customerApplicationService.verifyPhoneNumber(verifyCustomerPhoneCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerPhoneVerificationResponse(customerId)));
	}

	@Override
	@TracedOperation("addShippingAddress")
	public Mono<ResponseEntity<CustomerShippingAddressResponse>> addShippingAddress(
			UUID id, AddShippingAddressRequest request, ServerWebExchange exchange) {
			var addAddressCommand = CommandMapper.map(id, request);
			return customerApplicationService.addShippingAddress(addAddressCommand)
					.map(customerId -> ResponseEntity
							.status(HttpStatus.CREATED)
							.body(ResponseMapper.map(customerId, request)));
	}

	@Override
	@TracedOperation("updateShippingAddress")
	public Mono<ResponseEntity<CustomerShippingAddressResponse>> updateShippingAddress(
			UUID id, UUID addressId, UpdateShippingAddressRequest request, ServerWebExchange exchange) {

			var updateAddressCommand = CommandMapper.map(id, addressId, request);
			return customerApplicationService.updateShippingAddress(updateAddressCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(ResponseMapper.map(customerId, request)));
	}

	@Override
	@TracedOperation("removeShippingAddress")
	public Mono<ResponseEntity<Void>> removeShippingAddress(
			UUID id, UUID addressId, ServerWebExchange exchange) {
			var removeAddressCommand = CommandMapper.map(id, addressId);
			return customerApplicationService.removeShippingAddress(removeAddressCommand)
					.then(Mono.just(ResponseEntity.noContent().build()));
	}

	@Override
	@TracedOperation("updatePreferences")
	public Mono<ResponseEntity<CustomerPreferencesResponse>> updatePreferences(
			UUID id, UpdatePreferencesRequest request, ServerWebExchange exchange) {
			var updatePreferencesCommand = CommandMapper.map(id, request);
			return customerApplicationService.updatePreferences(updatePreferencesCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(ResponseMapper.map(customerId, request)));
	}

	@Override
	@TracedOperation("deactivate")
	public Mono<ResponseEntity<CustomerDeactivationResponse>> deactivate(
			UUID id, String reason, ServerWebExchange exchange) {
			var deactivateCommand = DeactivateCustomerCommand.builder()
					.customerId(id)
					.reason(reason)
					.build();
			return customerApplicationService.deactivate(deactivateCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerDeactivationResponse(customerId)));
	}

	@Override
	@TracedOperation("reactivate")
	public Mono<ResponseEntity<CustomerReactivationResponse>> reactivate(
			UUID id, String note, ServerWebExchange exchange) {
			var reactivateCommand = ReactivateCustomerCommand.builder()
					.customerId(id)
					.note(note)
					.build();
			return customerApplicationService.reactivate(reactivateCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerReactivationResponse(customerId)));
	}
}
