package pl.ecommerce.customer.write.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracedOperation;
import pl.ecommerce.customer.write.api.dto.*;
import pl.ecommerce.customer.write.api.mapper.CommandMapper;
import pl.ecommerce.customer.write.application.CustomerApplicationService;
import pl.ecommerce.customer.write.domain.commands.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static pl.ecommerce.customer.write.api.mapper.ResponseMapper.map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerController implements CustomerApi {

	private final CustomerApplicationService customerApplicationService;

	@Override
	@TracedOperation("registerCustomer")
	public Mono<CustomerRegistrationResponse> registerCustomer(
			CustomerRegistrationRequest request, ServerWebExchange exchange) {
		log.debug("Received registerCustomer request for email: {}", request.email());
		var registerCustomerCommand = CommandMapper.map(request);
		return customerApplicationService.registerCustomer(registerCustomerCommand)
				.map(customerAggregate -> {
					log.debug("Customer registered successfully: {}", customerAggregate);
					return map(customerAggregate);
				});
	}

	@Override
	@TracedOperation("updateCustomer")
	public Mono<Void> updateCustomer(
			UUID id, CustomerUpdateRequest request, ServerWebExchange exchange) {
		log.info("Received updateCustomer request for id: {}", id);
		var updateCustomerCommand = CommandMapper.map(id, request);
		return customerApplicationService.updateCustomer(updateCustomerCommand)
				.then();
	}

	@Override
	@TracedOperation("changeEmail")
	public Mono<Void> changeEmail(
			UUID id, String newEmail, ServerWebExchange exchange) {

			var changeCustomerEmailCommand = ChangeCustomerEmailCommand.builder()
					.customerId(id)
					.newEmail(newEmail)
					.build();
			return customerApplicationService.changeEmail(changeCustomerEmailCommand)
					.then();
		}

	@Override
	@TracedOperation("verifyEmail")
	public Mono<Void> verifyEmail(
			UUID id, String token, ServerWebExchange exchange) {

			var verifyCustomerEmailCommand = VerifyCustomerEmailCommand.builder()
					.customerId(id)
					.verificationToken(token)
					.build();
			return customerApplicationService.verifyEmail(verifyCustomerEmailCommand)
					.then();
	}

	@Override
	@TracedOperation("deleteCustomer")
	public Mono<Void> deleteCustomer(UUID id, ServerWebExchange exchange) {
			return customerApplicationService.deleteCustomer(id)
					.then();
	}

	@Override
	@TracedOperation("verifyPhoneNumber")
	public Mono<Void> verifyPhoneNumber(
			UUID id, String verificationToken, ServerWebExchange exchange) {
		var verifyCustomerPhoneCommand = VerifyCustomerPhoneCommand.builder()
				.customerId(id)
				.verificationToken(verificationToken)
				.build();
		return customerApplicationService.verifyPhoneNumber(verifyCustomerPhoneCommand)
				.then();
	}

	@Override
	@TracedOperation("addShippingAddress")
	public Mono<Void> addShippingAddress(
			UUID id, AddShippingAddressRequest request, ServerWebExchange exchange) {
			var addAddressCommand = CommandMapper.map(id, request);
			return customerApplicationService.addShippingAddress(addAddressCommand)
					.then();
	}

	@Override
	@TracedOperation("updateShippingAddress")
	public Mono<Void> updateShippingAddress(
			UUID id, UUID addressId, UpdateShippingAddressRequest request, ServerWebExchange exchange) {

			var updateAddressCommand = CommandMapper.map(id, addressId, request);
			return customerApplicationService.updateShippingAddress(updateAddressCommand)
					.then();
	}

	@Override
	@TracedOperation("removeShippingAddress")
	public Mono<Void> removeShippingAddress(
			UUID id, UUID addressId, ServerWebExchange exchange) {
			var removeAddressCommand = CommandMapper.map(id, addressId);
			return customerApplicationService.removeShippingAddress(removeAddressCommand)
					.then();
	}

	@Override
	@TracedOperation("updatePreferences")
	public Mono<Void> updatePreferences(
			UUID id, UpdatePreferencesRequest request, ServerWebExchange exchange) {
			var updatePreferencesCommand = CommandMapper.map(id, request);
			return customerApplicationService.updatePreferences(updatePreferencesCommand)
					.then();
	}

	@Override
	@TracedOperation("deactivate")
	public Mono<Void> deactivate(
			UUID id, String reason, ServerWebExchange exchange) {
			var deactivateCommand = DeactivateCustomerCommand.builder()
					.customerId(id)
					.reason(reason)
					.build();
			return customerApplicationService.deactivate(deactivateCommand)
					.then();
	}

	@Override
	@TracedOperation("reactivate")
	public Mono<Void> reactivate(
			UUID id, String note, ServerWebExchange exchange) {
			var reactivateCommand = ReactivateCustomerCommand.builder()
					.customerId(id)
					.note(note)
					.build();
			return customerApplicationService.reactivate(reactivateCommand)
					.then();
	}
}
