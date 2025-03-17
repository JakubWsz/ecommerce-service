package pl.ecommerce.customer.write.api;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.customer.write.api.dto.*;
import pl.ecommerce.customer.write.api.mapper.CommandMapper;
import pl.ecommerce.customer.write.api.mapper.ResponseMapper;
import pl.ecommerce.customer.write.application.CustomerApplicationService;
import pl.ecommerce.customer.write.domain.commands.ChangeCustomerEmailCommand;
import pl.ecommerce.customer.write.domain.commands.VerifyCustomerEmailCommand;
import pl.ecommerce.customer.write.domain.commands.VerifyCustomerPhoneCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customers", description = "Write operations for customer management")
public class CustomerController implements CustomerApi {

	private final CustomerApplicationService customerApplicationService;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<CustomerRegistrationResponse>> registerCustomer(
			CustomerRegistrationRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "registerCustomer");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to register customer with email: {}, traceId: {}",
				request.email(), traceId);

		var registerCustomerCommand = CommandMapper.map(request, tracingContext);

		return withObservation("registerCustomer", traceId,
				customerApplicationService.registerCustomer(registerCustomerCommand)
						.map(customerId -> ResponseMapper.map(registerCustomerCommand.customerId(), request, traceId)),
				HttpStatus.CREATED);
	}

	@Override
	public Mono<ResponseEntity<CustomerUpdateResponse>> updateCustomer(
			UUID id, CustomerUpdateRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "updateCustomer");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to update customer with id: {}, traceId: {}", id, traceId);

		var updateCustomerCommand = CommandMapper.map(id, request, tracingContext);

		return withObservation("updateCustomer", traceId,
				customerApplicationService.updateCustomer(updateCustomerCommand)
						.map(customerId -> ResponseMapper.map(customerId, request, traceId)),
				HttpStatus.OK);
	}

	@Override
	public Mono<ResponseEntity<CustomerEmailChangeResponse>> changeEmail(
			UUID id, String newEmail, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "changeEmail");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to change email for customer with id: {}, traceId: {}", id, traceId);

		var changeCustomerEmailCommand = new ChangeCustomerEmailCommand(id, newEmail, tracingContext);

		return withObservation("changeEmail", traceId,
				customerApplicationService.changeEmail(changeCustomerEmailCommand)
						.map(customerId -> new CustomerEmailChangeResponse(customerId, traceId, newEmail)),
				HttpStatus.OK);
	}

	@Override
	public Mono<ResponseEntity<CustomerVerificationResponse>> verifyEmail(
			UUID id, String token, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "verifyEmail");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to verify email for customer with id: {}, traceId: {}", id, traceId);

		var verifyCustomerEmailCommand = VerifyCustomerEmailCommand.builder()
				.customerId(id)
				.verificationToken(token)
				.tracingContext(tracingContext)
				.build();

		return withObservation("verifyEmail", traceId,
				customerApplicationService.verifyEmail(verifyCustomerEmailCommand)
						.map(customerId -> new CustomerVerificationResponse(customerId, traceId)),
				HttpStatus.OK);
	}

	@Override
	public Mono<ResponseEntity<Void>> deleteCustomer(UUID id, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "deleteCustomer");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to delete customer with id: {}, traceId: {}", id, traceId);

		return withObservation("deleteCustomer", traceId,
				customerApplicationService.deleteCustomer(id, tracingContext)
						.then(),
				HttpStatus.NO_CONTENT);
	}

	@Override
	public Mono<ResponseEntity<CustomerPhoneVerificationResponse>> verifyPhoneNumber(
			UUID id, String verificationToken, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "verifyPhoneNumber");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to verify phone number for customer with id: {}, traceId: {}", id, traceId);

		var verifyCustomerPhoneCommand = VerifyCustomerPhoneCommand.builder()
				.customerId(id)
				.verificationToken(verificationToken)
				.tracingContext(tracingContext)
				.build();

		return withObservation("verifyPhoneNumber", traceId,
				customerApplicationService.verifyPhoneNumber(verifyCustomerPhoneCommand)
						.map(customerId -> new CustomerPhoneVerificationResponse(customerId, traceId)),
				HttpStatus.OK);
	}

	@Override
	public Mono<ResponseEntity<CustomerShippingAddressResponse>> addShippingAddress(
			UUID id, AddShippingAddressRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "addShippingAddress");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to add shipping address for customer with id: {}, traceId: {}", id, traceId);

		var addAddressCommand = CommandMapper.map(id, request, tracingContext);

		return withObservation("addShippingAddress", traceId,
				customerApplicationService.addShippingAddress(addAddressCommand)
						.map(customerId -> ResponseMapper.map(customerId, request, traceId)),
				HttpStatus.CREATED);
	}

	@Override
	public Mono<ResponseEntity<CustomerShippingAddressResponse>> updateShippingAddress(
			UUID id, UUID addressId, UpdateShippingAddressRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "updateShippingAddress");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to update shipping address for customer with id: {}, addressId: {}, traceId: {}",
				id, addressId, traceId);

		var updateAddressCommand = CommandMapper.map(id, addressId, request, tracingContext);

		return withObservation("updateShippingAddress", traceId,
				customerApplicationService.updateShippingAddress(updateAddressCommand)
						.map(customerId -> ResponseMapper.map(customerId, request, traceId)),
				HttpStatus.OK);
	}

	@Override
	public Mono<ResponseEntity<Void>> removeShippingAddress(
			UUID id, UUID addressId, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "removeShippingAddress");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to remove shipping address for customer with id: {}, addressId: {}, traceId: {}",
				id, addressId, traceId);

		var removeAddressCommand = CommandMapper.map(id, addressId, tracingContext);

		return withObservation("removeShippingAddress", traceId,
				customerApplicationService.removeShippingAddress(removeAddressCommand)
						.then(),
				HttpStatus.NO_CONTENT);
	}

	@Override
	public Mono<ResponseEntity<CustomerPreferencesResponse>> updatePreferences(
			UUID id, UpdatePreferencesRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "updatePreferences");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to update preferences for customer with id: {}, traceId: {}", id, traceId);

		var updatePreferencesCommand = CommandMapper.map(id, request, tracingContext);

		return withObservation("updatePreferences", traceId,
				customerApplicationService.updatePreferences(updatePreferencesCommand)
						.map(customerId -> ResponseMapper.map(customerId, request, traceId)),
				HttpStatus.OK);
	}

	@Override
	public Mono<ResponseEntity<CustomerDeactivationResponse>> deactivate(
			UUID id, String reason, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "deactivate");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to deactivate customer with id: {}, reason: {}, traceId: {}", id, reason, traceId);

		var deactivateCommand = pl.ecommerce.customer.write.domain.commands.DeactivateCustomerCommand.builder()
				.customerId(id)
				.reason(reason)
				.tracingContext(tracingContext)
				.build();

		return withObservation("deactivate", traceId,
				customerApplicationService.deactivate(deactivateCommand)
						.map(customerId -> new CustomerDeactivationResponse(customerId, traceId)),
				HttpStatus.OK);
	}

	@Override
	public Mono<ResponseEntity<CustomerReactivationResponse>> reactivate(
			UUID id, String note, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "reactivate");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to reactivate customer with id: {}, note: {}, traceId: {}", id, note, traceId);

		var reactivateCommand = pl.ecommerce.customer.write.domain.commands.ReactivateCustomerCommand.builder()
				.customerId(id)
				.note(note)
				.tracingContext(tracingContext)
				.build();

		return withObservation("reactivate", traceId,
				customerApplicationService.reactivate(reactivateCommand)
						.map(customerId -> new CustomerReactivationResponse(customerId, traceId)),
				HttpStatus.OK);
	}

	private <T> Mono<ResponseEntity<T>> withObservation(String opName, String traceId, Mono<T> mono, HttpStatus status) {
		return Objects.requireNonNull(Observation.createNotStarted(opName, observationRegistry)
						.observe(() -> mono))
				.map(result -> ResponseEntity.status(status)
						.header("X-Trace-Id", traceId)
						.body(result));
	}

	private static TracingContext createTracingContext(ServerWebExchange exchange, String operation) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String traceId = headers.getFirst("X-Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}
		String userId = headers.getFirst("X-User-Id");
		return TracingContext.builder()
				.traceId(traceId)
				.spanId(UUID.randomUUID().toString())
				.userId(userId)
				.sourceService("customer-write")
				.sourceOperation(operation)
				.build();
	}
}
