package pl.ecommerce.vendor.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.infrastructure.VendorValidator;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorAlreadyExistsException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static pl.ecommerce.vendor.infrastructure.VendorEventUtils.*;
import static pl.ecommerce.vendor.infrastructure.VendorValidator.isValidVerificationStatus;
import static pl.ecommerce.vendor.infrastructure.VendorValidator.validateGdprConsent;
import static pl.ecommerce.vendor.infrastructure.constant.VendorConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

	private final VendorRepository vendorRepository;
	private final EventPublisher eventPublisher;

	@Transactional
	public Mono<Vendor> registerVendor(Vendor vendor) {
		log.info(LOG_OPERATION_STARTED, "Vendor registration", "email", vendor.getEmail());

		return validateGdprConsent(vendor)
				.then(checkIfVendorExists(vendor.getEmail()))
				.then(Mono.defer(() -> createAndSaveVendor(vendor)))
				.flatMap(savedVendor -> publishVendorRegisteredEvent(eventPublisher, savedVendor)
						.doOnSuccess(v -> log.info(LOG_ENTITY_CREATED, "Vendor", savedVendor.getId()))
						.doOnError(e -> log.error(LOG_ERROR, "vendor registration", e.getMessage(), e)));
	}

	public Mono<Vendor> getVendorById(UUID id) {
		log.debug(LOG_OPERATION_STARTED, "Vendor retrieval", "id", id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.doOnSuccess(vendor -> log.debug(LOG_OPERATION_COMPLETED, "Vendor retrieval", "id", id))
				.doOnError(e -> log.error(LOG_ERROR, "vendor retrieval", e.getMessage(), e));
	}

	public Flux<Vendor> getAllVendors() {
		log.debug(LOG_OPERATION_STARTED, "All vendors retrieval", "", "");

		return vendorRepository.findByActiveTrue()
				.doOnComplete(() -> log.debug(LOG_OPERATION_COMPLETED, "All vendors retrieval", "", ""));
	}

	@Transactional
	public Mono<Vendor> updateVendor(UUID id, Vendor vendorUpdate) {
		log.info(LOG_OPERATION_STARTED, "Vendor update", "id", id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(existingVendor -> updateVendorData(vendorUpdate, existingVendor))
				.doOnSuccess(vendor -> log.info(LOG_ENTITY_UPDATED, "Vendor", vendor.getId()))
				.doOnError(e -> log.error(LOG_ERROR, "vendor update", e.getMessage(), e));
	}

	@Transactional
	public Mono<Vendor> updateVendorStatus(UUID id, Vendor.VendorStatus status, String reason) {
		log.info(LOG_OPERATION_STARTED, "Vendor status update", "id", id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(existingVendor -> processVendorStatusUpdate(existingVendor, status, reason))
				.doOnSuccess(vendor -> log.info(LOG_ENTITY_UPDATED, "Vendor status", vendor.getId()))
				.doOnError(e -> log.error(LOG_ERROR, "vendor status update", e.getMessage(), e));
	}

	@Transactional
	public Mono<Vendor> updateVerificationStatus(UUID id, Vendor.VerificationStatus status) {
		log.info(LOG_OPERATION_STARTED, "Verification status update", "vendor", id);

		if (isValidVerificationStatus(status)) {
			return Mono.error(new ValidationException(ERROR_INVALID_STATUS + status));
		}

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(vendor -> updateAndSaveVendor(vendor, status)
						.switchIfEmpty(Mono.error(new IllegalStateException("updateAndSaveVendor returned null!"))))
				.doOnSuccess(vendor -> log.info(LOG_ENTITY_UPDATED, "Verification status", vendor.getId()))
				.doOnError(e -> log.error(LOG_ERROR, "verification status update", e.getMessage(), e));
	}

	@Transactional
	public Mono<Void> deactivateVendor(UUID id) {
		log.info(LOG_OPERATION_STARTED, "Vendor deactivation", "id", id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(this::deactivateVendor)
				.doOnSuccess(v -> {
					log.info(LOG_ENTITY_UPDATED, "Vendor deactivated", id);
					publishVendorStatusChangedEvent(eventPublisher, null, v);
				})
				.doOnError(e -> log.error(LOG_ERROR, "vendor deactivation", e.getMessage(), e))
				.then();
	}

	private Mono<Vendor> deactivateVendor(Vendor vendor) {
		vendor.setActive(false);
		return vendorRepository.save(vendor);
	}

	private Mono<Vendor> createAndSaveVendor(Vendor vendor) {
		Vendor newVendor = initializeNewVendor(vendor);
		return VendorValidator.validateVendor(newVendor)
				.then(vendorRepository.save(newVendor));
	}

	private Mono<Vendor> processVendorStatusUpdate(Vendor existingVendor, Vendor.VendorStatus status, String reason) {
		return Mono.defer(() -> {
			try {
				Vendor updatedVendor = switch (status) {
					case ACTIVE -> VendorValidator.activateVendor(existingVendor);
					case SUSPENDED -> VendorValidator.suspendVendor(existingVendor, reason);
					case BANNED -> VendorValidator.banVendor(existingVendor, reason);
					default -> throw new ValidationException(ERROR_INVALID_STATUS + status);
				};

				return vendorRepository.save(updatedVendor)
						.doOnSuccess(saved -> publishVendorStatusChangedEvent(eventPublisher, reason, saved));
			} catch (ValidationException e) {
				return Mono.error(e);
			}
		});
	}

	private Mono<Vendor> updateAndSaveVendor(Vendor vendor, Vendor.VerificationStatus status) {
		return Mono.just(vendor)
				.map(v -> VendorValidator.updateVerificationStatus(v, status))
				.flatMap(vendorRepository::save)
				.doOnSuccess(v -> publishVendorVerificationEvent(eventPublisher, v));
	}

	private Mono<Void> checkIfVendorExists(String email) {
		return vendorRepository.existsByEmail(email)
				.flatMap(exists -> throwIfExists(email, exists));
	}

	private static Mono<Void> throwIfExists(String email, Boolean exists) {
		if (exists) {
			log.warn("Vendor with email {} already exists", email);
			return Mono.error(new VendorAlreadyExistsException(ERROR_VENDOR_ALREADY_EXISTS.replace("{}", email)));
		}
		return Mono.empty();
	}

	private Mono<Vendor> updateVendorData(Vendor vendorUpdate, Vendor existingVendor) {
		Map<String, Object> changes = new HashMap<>();

		updateFieldIfPresent(vendorUpdate.getName(), existingVendor::setName, "name", changes);
		updateFieldIfPresent(vendorUpdate.getDescription(), existingVendor::setDescription, "description", changes);
		updateFieldIfPresent(vendorUpdate.getPhone(), existingVendor::setPhone, "phone", changes);
		updateFieldIfPresent(vendorUpdate.getBusinessName(), existingVendor::setBusinessName, "businessName", changes);
		updateFieldIfPresent(vendorUpdate.getTaxId(), existingVendor::setTaxId, "taxId", changes);
		updateFieldIfPresent(vendorUpdate.getBusinessAddress(), existingVendor::setBusinessAddress, "businessAddress", changes);
		updateFieldIfPresent(vendorUpdate.getBankAccountDetails(), existingVendor::setBankAccountDetails, "bankAccountDetails", changes);

		return vendorRepository.save(existingVendor)
				.doOnSuccess(vendor -> {
					log.info(LOG_ENTITY_UPDATED, "Vendor", vendor.getId());
					publishVendorUpdatedEvent(eventPublisher, vendor.getId(), changes);
				});
	}

	public static Vendor initializeNewVendor(Vendor requestVendor) {
		LocalDateTime now = LocalDateTime.now();

		return Vendor.builder()
				.name(requestVendor.getName())
				.description(requestVendor.getDescription())
				.email(requestVendor.getEmail())
				.phone(requestVendor.getPhone())
				.businessName(requestVendor.getBusinessName())
				.taxId(requestVendor.getTaxId())
				.businessAddress(requestVendor.getBusinessAddress())
				.bankAccountDetails(requestVendor.getBankAccountDetails())
				.gdprConsent(requestVendor.getGdprConsent())
				.consentTimestamp(requestVendor.getGdprConsent() ? now : null)
				.build();
	}

	public void publishVendorStatusChangedEvent(EventPublisher eventPublisher, String reason, Vendor vendor) {
		var event = createVendorStatusChangedEvent(vendor, vendor.getVendorStatus(), reason);
		eventPublisher.publish(event);
		log.info("Vendor status updated: {}", vendor.getId());
	}

	public static void publishVendorVerificationEvent(EventPublisher eventPublisher, Vendor vendor) {
		var event = createVendorVerificationCompletedEvent(vendor);
		eventPublisher.publish(event);
		log.info("Vendor verification updated: {}", vendor.getId());
	}


	public static Mono<Vendor> publishVendorRegisteredEvent(EventPublisher eventPublisher, Vendor vendor) {
		var event = createVendorRegisteredEvent(vendor);
		eventPublisher.publish(event);
		log.info("Vendor registered: {}", vendor.getId());
		return Mono.just(vendor);
	}

	public static void publishVendorUpdatedEvent(EventPublisher eventPublisher, UUID vendorId, Map<String, Object> changes) {
		var event = createVendorUpdatedEvent(vendorId,changes);
		log.info("Vendor data updated: {}", vendorId);
		eventPublisher.publish(event);
	}

	public static <T> void updateFieldIfPresent(T newValue, Consumer<T> setter, String fieldName, Map<String, Object> changes) {
		if (newValue != null) {
			setter.accept(newValue);
			changes.put(fieldName, newValue);
		}
	}
}