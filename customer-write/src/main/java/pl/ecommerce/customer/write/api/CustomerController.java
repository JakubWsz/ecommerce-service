package pl.ecommerce.customer.write.api;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TraceService;
import pl.ecommerce.commons.tracing.TracedOperation;
import pl.ecommerce.commons.tracing.TracingAspect;
import pl.ecommerce.customer.write.api.dto.*;
import pl.ecommerce.customer.write.api.mapper.CommandMapper;
import pl.ecommerce.customer.write.api.mapper.ResponseMapper;
import pl.ecommerce.customer.write.application.CustomerApplicationService;
import pl.ecommerce.customer.write.domain.commands.*;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

import static java.util.Objects.nonNull;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerController implements CustomerApi {

	private final CustomerApplicationService customerApplicationService;
	private final TraceService traceService;

	@Override
	@TracedOperation("registerCustomer")
	public Mono<ResponseEntity<CustomerRegistrationResponse>> registerCustomer(
			CustomerRegistrationRequest request, ServerWebExchange exchange) {

		return executeWithSpan("register-customer", traceId -> {
			// Dodaj atrybuty do bieżącego spanu
			Span currentSpan = Span.current();
			currentSpan.setAttribute("request.email", request.email());

			String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
			if (nonNull(userId)) {
				currentSpan.setAttribute("user.id", userId);
			}

			log.info("Received request to register customer with email: {}, traceId: {}",
					request.email(), traceId);

			var registerCustomerCommand = CommandMapper.map(request);
			return customerApplicationService.registerCustomer(registerCustomerCommand)
					.map(customerId -> ResponseEntity
							.status(HttpStatus.CREATED)
							.body(ResponseMapper.map(customerId, request, traceId)));
		});
	}

	@Override
	@TracedOperation("updateCustomer")
	public Mono<ResponseEntity<CustomerUpdateResponse>> updateCustomer(
			UUID id, CustomerUpdateRequest request, ServerWebExchange exchange) {

		return executeWithSpan("update-customer", traceId -> {
			// Dodaj atrybuty do bieżącego spanu
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());

			log.info("Received request to update customer with id: {}, traceId: {}", id, traceId);

			var updateCustomerCommand = CommandMapper.map(id, request);
			return customerApplicationService.updateCustomer(updateCustomerCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(ResponseMapper.map(customerId, request, traceId)));
		});
	}

	@Override
	@TracedOperation("changeEmail")
	public Mono<ResponseEntity<CustomerEmailChangeResponse>> changeEmail(
			UUID id, String newEmail, ServerWebExchange exchange) {

		return executeWithSpan("change-email", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());
			currentSpan.setAttribute("new_email", newEmail);

			log.info("Received request to change email for customer with id: {}, traceId: {}",
					id, traceId);

			var changeCustomerEmailCommand = ChangeCustomerEmailCommand.builder()
					.customerId(id)
					.newEmail(newEmail)
					.build();

			return customerApplicationService.changeEmail(changeCustomerEmailCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerEmailChangeResponse(customerId, traceId, newEmail)));
		});
	}

	@Override
	@TracedOperation("verifyEmail")
	public Mono<ResponseEntity<CustomerVerificationResponse>> verifyEmail(
			UUID id, String token, ServerWebExchange exchange) {

		return executeWithSpan("verify-email", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());
			currentSpan.setAttribute("operation", "verify_email");

			log.info("Received request to verify email for customer with id: {}, traceId: {}",
					id, traceId);

			var verifyCustomerEmailCommand = VerifyCustomerEmailCommand.builder()
					.customerId(id)
					.verificationToken(token)
					.build();

			return customerApplicationService.verifyEmail(verifyCustomerEmailCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerVerificationResponse(customerId, traceId)));
		});
	}

	@Override
	@TracedOperation("deleteCustomer")
	public Mono<ResponseEntity<Void>> deleteCustomer(UUID id, ServerWebExchange exchange) {

		return executeWithSpan("delete-customer", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());

			log.info("Received request to delete customer with id: {}, traceId: {}", id, traceId);

			return customerApplicationService.deleteCustomer(id)
					.then(Mono.just(ResponseEntity.noContent().build()));
		});
	}

	@Override
	@TracedOperation("verifyPhoneNumber")
	public Mono<ResponseEntity<CustomerPhoneVerificationResponse>> verifyPhoneNumber(
			UUID id, String verificationToken, ServerWebExchange exchange) {

		return executeWithSpan("verify-phone", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());

			log.info("Received request to verify phone number for customer with id: {}, traceId: {}",
					id, traceId);

			var verifyCustomerPhoneCommand = VerifyCustomerPhoneCommand.builder()
					.customerId(id)
					.verificationToken(verificationToken)
					.build();

			return customerApplicationService.verifyPhoneNumber(verifyCustomerPhoneCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerPhoneVerificationResponse(customerId, traceId)));
		});
	}

	@Override
	@TracedOperation("addShippingAddress")
	public Mono<ResponseEntity<CustomerShippingAddressResponse>> addShippingAddress(
			UUID id, AddShippingAddressRequest request, ServerWebExchange exchange) {

		return executeWithSpan("add-shipping-address", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());

			log.info("Received request to add shipping address for customer with id: {}, traceId: {}",
					id, traceId);

			var addAddressCommand = CommandMapper.map(id, request);

			return customerApplicationService.addShippingAddress(addAddressCommand)
					.map(customerId -> ResponseEntity
							.status(HttpStatus.CREATED)
							.body(ResponseMapper.map(customerId, request, traceId)));
		});
	}

	@Override
	@TracedOperation("updateShippingAddress")
	public Mono<ResponseEntity<CustomerShippingAddressResponse>> updateShippingAddress(
			UUID id, UUID addressId, UpdateShippingAddressRequest request, ServerWebExchange exchange) {

		return executeWithSpan("update-shipping-address", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());
			currentSpan.setAttribute("address.id", addressId.toString());

			log.info("Received request to update shipping address for customer with id: {}, addressId: {}, traceId: {}",
					id, addressId, traceId);

			var updateAddressCommand = CommandMapper.map(id, addressId, request);

			return customerApplicationService.updateShippingAddress(updateAddressCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(ResponseMapper.map(customerId, request, traceId)));
		});
	}

	@Override
	@TracedOperation("removeShippingAddress")
	public Mono<ResponseEntity<Void>> removeShippingAddress(
			UUID id, UUID addressId, ServerWebExchange exchange) {

		return executeWithSpan("remove-shipping-address", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());
			currentSpan.setAttribute("address.id", addressId.toString());

			log.info("Received request to remove shipping address for customer with id: {}, addressId: {}, traceId: {}",
					id, addressId, traceId);

			var removeAddressCommand = CommandMapper.map(id, addressId);

			return customerApplicationService.removeShippingAddress(removeAddressCommand)
					.then(Mono.just(ResponseEntity.noContent().build()));
		});
	}

	@Override
	@TracedOperation("updatePreferences")
	public Mono<ResponseEntity<CustomerPreferencesResponse>> updatePreferences(
			UUID id, UpdatePreferencesRequest request, ServerWebExchange exchange) {

		return executeWithSpan("update-preferences", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());

			log.info("Received request to update preferences for customer with id: {}, traceId: {}",
					id, traceId);

			var updatePreferencesCommand = CommandMapper.map(id, request);

			return customerApplicationService.updatePreferences(updatePreferencesCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(ResponseMapper.map(customerId, request, traceId)));
		});
	}

	@Override
	@TracedOperation("deactivate")
	public Mono<ResponseEntity<CustomerDeactivationResponse>> deactivate(
			UUID id, String reason, ServerWebExchange exchange) {

		return executeWithSpan("deactivate-customer", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());
			if (nonNull(reason)) {
				currentSpan.setAttribute("reason", reason);
			}

			log.info("Received request to deactivate customer with id: {}, reason: {}, traceId: {}",
					id, reason, traceId);

			var deactivateCommand = DeactivateCustomerCommand.builder()
					.customerId(id)
					.reason(reason)
					.build();

			return customerApplicationService.deactivate(deactivateCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerDeactivationResponse(customerId, traceId)));
		});
	}

	@Override
	@TracedOperation("reactivate")
	public Mono<ResponseEntity<CustomerReactivationResponse>> reactivate(
			UUID id, String note, ServerWebExchange exchange) {

		return executeWithSpan("reactivate-customer", traceId -> {
			Span currentSpan = Span.current();
			currentSpan.setAttribute("customer.id", id.toString());
			if (nonNull(note)) {
				currentSpan.setAttribute("note", note);
			}

			log.info("Received request to reactivate customer with id: {}, note: {}, traceId: {}",
					id, note, traceId);

			var reactivateCommand = ReactivateCustomerCommand.builder()
					.customerId(id)
					.note(note)
					.build();

			return customerApplicationService.reactivate(reactivateCommand)
					.map(customerId -> ResponseEntity
							.ok()
							.body(new CustomerReactivationResponse(customerId, traceId)));
		});
	}

	private <T> Mono<T> executeWithSpan(String operation, Function<String, Mono<T>> action) {
		return Mono.deferContextual(contextView -> {
			String traceId = contextView.getOrDefault(TracingAspect.TRACE_ID_CONTEXT_KEY,
					traceService.getCurrentTraceId());

			Span span = io.opentelemetry.api.GlobalOpenTelemetry.getTracer("customer-write")
					.spanBuilder(operation)
					.setSpanKind(SpanKind.INTERNAL)
					.startSpan();

			try (io.opentelemetry.context.Scope scope = span.makeCurrent()) {
				return action.apply(traceId)
						.doOnTerminate(span::end)
						.doOnError(span::recordException);
			}
		});
	}
}