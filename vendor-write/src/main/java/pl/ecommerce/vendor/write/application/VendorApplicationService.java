package pl.ecommerce.vendor.write.application;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.domain.command.*;
import pl.ecommerce.vendor.write.infrastructure.exception.VendorAlreadyExistsException;
import pl.ecommerce.vendor.write.infrastructure.exception.VendorNotFoundException;
import pl.ecommerce.vendor.write.infrastructure.repository.VendorRepository;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorApplicationService {

	private final VendorRepository vendorRepository;
	private final ObservationRegistry observationRegistry;

	public Mono<UUID> registerVendor(RegisterVendorCommand command) {
		String traceId = getTraceId(command.tracingContext());
		log.info("Registering new vendor with email: {}, traceId: {}", command.email(), traceId);

		return Observation.createNotStarted("vendor.register", observationRegistry)
				.observe(() -> processRegistration(command, traceId));
	}

	public Mono<UUID> updateVendor(UpdateVendorCommand command) {
		UUID vendorId = command.vendorId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Updating vendor with ID: {}, traceId: {}", vendorId, traceId);

		return modifyVendor(vendorId, "vendor.update",
				"Vendor updated successfully: {}, traceId: {}", traceId,
				vendor -> vendor.updateVendor(command));
	}

	public Mono<UUID> verifyVendor(VerifyVendorCommand command) {
		UUID vendorId = command.vendorId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Verifying vendor with ID: {}, traceId: {}", vendorId, traceId);

		return modifyVendor(vendorId, "vendor.verify",
				"Vendor verified successfully: {}, traceId: {}", traceId,
				vendor -> vendor.verifyVendor(command));
	}

	public Mono<UUID> changeVendorStatus(ChangeVendorStatusCommand command) {
		UUID vendorId = command.vendorId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Changing status for vendor with ID: {} to {}, traceId: {}",
				vendorId, command.newStatus(), traceId);

		return modifyVendor(vendorId, "vendor.changeStatus",
				"Vendor status changed successfully: {}, traceId: {}", traceId,
				vendor -> vendor.changeStatus(command));
	}

	public Mono<UUID> addCategory(AddCategoryCommand command) {
		UUID vendorId = command.vendorId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Adding category {} to vendor: {}, traceId: {}",
				command.categoryId(), vendorId, traceId);

		return modifyVendor(vendorId, "vendor.addCategory",
				"Category added successfully to vendor: {}, traceId: {}", traceId,
				vendor -> vendor.addCategory(command));
	}

	public Mono<UUID> removeCategory(RemoveCategoryCommand command) {
		UUID vendorId = command.vendorId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Removing category {} from vendor: {}, traceId: {}",
				command.categoryId(), vendorId, traceId);

		return modifyVendor(vendorId, "vendor.removeCategory",
				"Category removed successfully from vendor: {}, traceId: {}", traceId,
				vendor -> vendor.removeCategory(command));
	}

	public Mono<UUID> updateBankDetails(UpdateBankDetailsCommand command) {
		UUID vendorId = command.vendorId();
		String traceId = getTraceId(command.tracingContext());
		log.info("Updating bank details for vendor: {}, traceId: {}", vendorId, traceId);

		return modifyVendor(vendorId, "vendor.updateBankDetails",
				"Bank details updated successfully for vendor: {}, traceId: {}", traceId,
				vendor -> vendor.updateBankDetails(command));
	}

	public Mono<UUID> deleteVendor(UUID vendorId, TracingContext tracingContext) {
		String traceId = getTraceId(tracingContext);
		log.info("Deleting vendor with ID: {}, traceId: {}", vendorId, traceId);

		return modifyVendor(vendorId, "vendor.delete",
				"Vendor deleted successfully: {}, traceId: {}",
				traceId,
				vendor -> vendor.delete(DeleteVendorCommand.builder()
						.vendorId(vendorId)
						.reason("User requested deletion")
						.tracingContext(tracingContext)
						.build()));
	}

	private Mono<UUID> processRegistration(RegisterVendorCommand command, String traceId) {
		return vendorRepository.existsByEmail(command.email())
				.flatMap(exists -> {
					if (exists) {
						log.warn("Vendor with email {} already exists, traceId: {}", command.email(), traceId);
						return Mono.error(new VendorAlreadyExistsException("Vendor with email already exists", traceId));
					}

					UUID vendorId = command.vendorId() != null ? command.vendorId() : UUID.randomUUID();
					VendorAggregate vendor = new VendorAggregate(command);
					return vendorRepository.save(vendor)
							.doOnSuccess(savedVendor -> log.info("Vendor registered successfully: {}, traceId: {}",
									vendorId, traceId))
							.map(VendorAggregate::getId);
				});
	}

	private Mono<VendorAggregate> loadVendorAggregate(UUID vendorId) {
		return vendorRepository.findById(vendorId)
				.switchIfEmpty(Mono.error(new VendorNotFoundException("Vendor not found with ID: " + vendorId)));
	}

	private Mono<UUID> modifyVendor(UUID vendorId,
									String observationName,
									String successMessage,
									String traceId,
									Consumer<VendorAggregate> updater) {
		return Objects.requireNonNull(Observation.createNotStarted(observationName, observationRegistry)
						.observe(() -> loadVendorAggregate(vendorId)
								.flatMap(vendor -> {
									updater.accept(vendor);
									return vendorRepository.save(vendor)
											.doOnSuccess(savedVendor -> log.info(successMessage, vendorId, traceId));
								})))
				.map(VendorAggregate::getId);
	}

	private String getTraceId(TracingContext tracingContext) {
		return tracingContext != null ? tracingContext.getTraceId() : "unknown";
	}
}