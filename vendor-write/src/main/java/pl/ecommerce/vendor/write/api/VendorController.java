package pl.ecommerce.vendor.write.api;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.vendor.write.api.dto.request.*;
import pl.ecommerce.vendor.write.api.dto.response.*;
import pl.ecommerce.vendor.write.application.VendorApplicationService;
import pl.ecommerce.vendor.write.domain.command.*;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vendors", description = "Write operations for vendor management")
public class VendorController implements VendorApi {

	private final VendorApplicationService vendorApplicationService;
	private final ObservationRegistry observationRegistry;

	@Override
	public Mono<ResponseEntity<VendorRegistrationResponse>> registerVendor(
			@Valid VendorRegistrationRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "registerVendor");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to register vendor: {}, traceId: {}", request.getEmail(), traceId);

		RegisterVendorCommand command = RegisterVendorCommand.builder()
				.vendorId(UUID.randomUUID())
				.name(request.getName())
				.businessName(request.getBusinessName())
				.taxId(request.getTaxId())
				.email(request.getEmail())
				.phone(request.getPhone())
				.legalForm(request.getLegalForm())
				.initialCategories(request.getInitialCategories())
				.commissionRate(request.getCommissionRate())
				.tracingContext(tracingContext)
				.build();

		return withObservation("registerVendor", traceId,
				vendorApplicationService.registerVendor(command)
						.map(vendorId -> VendorRegistrationResponse.builder()
								.vendorId(vendorId)
								.email(request.getEmail())
								.status("PENDING")
								.message("Vendor registration successful. Awaiting verification.")
								.build())
						.map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response)));
	}

	@Override
	public Mono<ResponseEntity<VendorUpdateResponse>> updateVendor(
			UUID id, @Valid VendorUpdateRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "updateVendor");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to update vendor with id: {}, traceId: {}", id, traceId);

		UpdateVendorCommand command = UpdateVendorCommand.builder()
				.vendorId(id)
				.name(request.getName())
				.businessName(request.getBusinessName())
				.phone(request.getPhone())
				.contactPersonName(request.getContactPersonName())
				.contactPersonEmail(request.getContactPersonEmail())
				.tracingContext(tracingContext)
				.build();

		return withObservation("updateVendor", traceId,
				vendorApplicationService.updateVendor(command)
						.map(vendorId -> VendorUpdateResponse.builder()
								.vendorId(vendorId)
								.message("Vendor updated successfully")
								.build())
						.map(ResponseEntity::ok));
	}

	@Override
	public Mono<ResponseEntity<CategoryAssignmentResponse>> addCategory(
			UUID id, @Valid AddCategoryRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "addCategory");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to add category to vendor: {}, category: {}, traceId: {}",
				id, request.getCategoryId(), traceId);

		AddCategoryCommand command = AddCategoryCommand.builder()
				.vendorId(id)
				.categoryId(request.getCategoryId())
				.categoryName(request.getCategoryName())
				.tracingContext(tracingContext)
				.build();

		return withObservation("addCategory", traceId,
				vendorApplicationService.addCategory(command)
						.map(vendorId -> CategoryAssignmentResponse.builder()
								.vendorId(vendorId)
								.categoryId(request.getCategoryId())
								.message("Category assigned successfully")
								.build())
						.map(ResponseEntity::ok));
	}

	@Override
	public Mono<ResponseEntity<Void>> removeCategory(
			UUID id, UUID categoryId, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "removeCategory");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to remove category from vendor: {}, category: {}, traceId: {}",
				id, categoryId, traceId);

		RemoveCategoryCommand command = RemoveCategoryCommand.builder()
				.vendorId(id)
				.categoryId(categoryId)
				.tracingContext(tracingContext)
				.build();

		return withObservation("removeCategory", traceId,
				vendorApplicationService.removeCategory(command)
						.then(Mono.just(ResponseEntity.noContent().build())));
	}

	@Override
	public Mono<ResponseEntity<VendorUpdateResponse>> changeVendorStatus(
			UUID id, @Valid VendorStatusChangeRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "changeVendorStatus");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to change vendor status: {}, new status: {}, traceId: {}",
				id, request.getNewStatus(), traceId);

		ChangeVendorStatusCommand command = ChangeVendorStatusCommand.builder()
				.vendorId(id)
				.newStatus(VendorStatus.valueOf(request.getNewStatus()))
				.reason(request.getReason())
				.tracingContext(tracingContext)
				.build();

		return withObservation("changeVendorStatus", traceId,
				vendorApplicationService.changeVendorStatus(command)
						.map(vendorId -> VendorUpdateResponse.builder()
								.vendorId(vendorId)
								.message("Vendor status changed to " + request.getNewStatus())
								.build())
						.map(ResponseEntity::ok));
	}

	@Override
	public Mono<ResponseEntity<VendorVerificationResponse>> verifyVendor(
			UUID id, @Valid VendorVerificationRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "verifyVendor");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to verify vendor: {}, verification id: {}, traceId: {}",
				id, request.getVerificationId(), traceId);

		VerifyVendorCommand command = VerifyVendorCommand.builder()
				.vendorId(id)
				.verificationId(request.getVerificationId())
				.verifiedFields(request.getVerifiedFields())
				.tracingContext(tracingContext)
				.build();

		return withObservation("verifyVendor", traceId,
				vendorApplicationService.verifyVendor(command)
						.map(vendorId -> VendorVerificationResponse.builder()
								.vendorId(vendorId)
								.verificationId(request.getVerificationId())
								.status("APPROVED")
								.message("Vendor verification completed successfully")
								.build())
						.map(ResponseEntity::ok));
	}

	@Override
	public Mono<ResponseEntity<BankDetailsResponse>> updateBankDetails(
			UUID id, @Valid UpdateBankDetailsRequest request, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "updateBankDetails");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to update bank details for vendor: {}, traceId: {}", id, traceId);

		UpdateBankDetailsCommand command = UpdateBankDetailsCommand.builder()
				.vendorId(id)
				.bankAccountNumber(request.getBankAccountNumber())
				.bankName(request.getBankName())
				.bankSwiftCode(request.getBankSwiftCode())
				.tracingContext(tracingContext)
				.build();

		return withObservation("updateBankDetails", traceId,
				vendorApplicationService.updateBankDetails(command)
						.map(vendorId -> BankDetailsResponse.builder()
								.vendorId(vendorId)
								.message("Bank details updated successfully")
								.build())
						.map(ResponseEntity::ok));
	}

	@Override
	public Mono<ResponseEntity<Void>> deleteVendor(UUID id, ServerWebExchange exchange) {

		TracingContext tracingContext = createTracingContext(exchange, "deleteVendor");
		String traceId = tracingContext.getTraceId();
		log.info("Received request to delete vendor: {}, traceId: {}", id, traceId);

		return withObservation("deleteVendor", traceId,
				vendorApplicationService.deleteVendor(id, tracingContext)
						.then(Mono.just(ResponseEntity.noContent().build())));
	}

	private <T> Mono<T> withObservation(String opName, String traceId, Mono<T> mono) {
		return Objects.requireNonNull(Observation.createNotStarted(opName, observationRegistry)
						.observe(() -> mono))
				.doOnError(error -> log.error("Error in operation {}: {}, traceId: {}",
						opName, error.getMessage(), traceId));
	}

	private TracingContext createTracingContext(ServerWebExchange exchange, String operation) {
		String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}
		String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
		return TracingContext.builder()
				.traceId(traceId)
				.spanId(UUID.randomUUID().toString())
				.userId(userId)
				.sourceService("vendor-write")
				.sourceOperation(operation)
				.build();
	}
}