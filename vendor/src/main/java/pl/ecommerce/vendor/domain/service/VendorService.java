package pl.ecommerce.vendor.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.vendor.domain.model.Vendor;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import pl.ecommerce.vendor.domain.repository.VendorRepository;
import pl.ecommerce.vendor.infrastructure.VendorValidator;
import pl.ecommerce.vendor.infrastructure.exception.ValidationException;
import pl.ecommerce.vendor.infrastructure.exception.VendorAlreadyExistsException;
import pl.ecommerce.vendor.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

import static pl.ecommerce.vendor.infrastructure.VendorValidator.isValidVerificationStatus;
import static pl.ecommerce.vendor.infrastructure.VendorValidator.validateGdprConsent;
import static pl.ecommerce.vendor.infrastructure.utils.VendorCategoryUtils.assignCategoriesAndPublishEvent;
import static pl.ecommerce.vendor.infrastructure.utils.VendorEventPublisherUtils.*;
import static pl.ecommerce.vendor.infrastructure.utils.VendorServiceConstants.*;
import static pl.ecommerce.vendor.infrastructure.utils.VendorUpdateUtils.updateFieldIfPresent;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

	private final VendorRepository vendorRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final EventPublisher eventPublisher;

	@Transactional
	public Mono<Vendor> registerVendor(Vendor vendor) {
		log.info(LOG_REGISTERING_VENDOR, vendor.getEmail());

		return validateGdprConsent(vendor)
				.then(checkIfVendorExists(vendor.getEmail()))
				.then(Mono.defer(() -> createAndSaveVendor(vendor)))
				.flatMap(v -> assignCategoriesAndPublishEvent(vendor, categoryAssignmentRepository, eventPublisher))
				.doOnError(e -> log.error(LOG_ERROR_SAVING_VENDOR, e.getMessage(), e));
	}

	public Mono<Vendor> getVendorById(UUID id) {
		log.debug(LOG_FETCHING_VENDOR, id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.doOnNext(c -> log.debug(LOG_VENDOR_FOUND, c))
				.doOnError(e -> log.error(ERROR_FETCHING_VENDOR, e.getMessage()));
	}

	public Flux<Vendor> getAllVendors() {
		log.debug(LOG_FETCHING_ALL_VENDORS);
		return vendorRepository.findByActiveTrue();
	}

	@Transactional
	public Mono<Vendor> updateVendor(UUID id, Vendor vendorUpdate) {
		logUpdatingVendor(id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(existingVendor -> updateVendorData(vendorUpdate, existingVendor));
	}

	@Transactional
	public Mono<Vendor> updateVendorStatus(UUID id, Vendor.VendorStatus status, String reason) {
		logUpdatingVendor(id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(existingVendor -> processVendorStatusUpdate(existingVendor, status, reason));
	}

	@Transactional
	public Mono<Vendor> updateVerificationStatus(UUID id, Vendor.VendorVerificationStatus status) {
		log.info(LOG_UPDATING_VERIFICATION_STATUS, id, status);

		if (isValidVerificationStatus(status)) {
			return Mono.error(new ValidationException("Invalid verification status: " + status));
		}

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(vendor -> updateAndSaveVendor(vendor, status));
	}

	@Transactional
	public Mono<Void> deactivateVendor(UUID id) {
		log.info(LOG_DEACTIVATING_VENDOR, id);

		return vendorRepository.findById(id)
				.switchIfEmpty(Mono.error(new VendorNotFoundException(ERROR_VENDOR_NOT_FOUND + id)))
				.flatMap(this::deactivateVendor)
				.doOnSuccess(v -> publishVendorStatusChangedEvent(eventPublisher, null, v))
				.then();
	}

	private Mono<Vendor> deactivateVendor(Vendor vendor) {
		vendor.setActive(false);
		vendor.setUpdatedAt(LocalDateTime.now());
		return vendorRepository.save(vendor);
	}

	private Mono<Vendor> createAndSaveVendor(Vendor vendor) {
		Vendor newVendor = VendorValidator.initializeNewVendor(vendor);
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
					default -> throw new ValidationException("Invalid status: " + status);
				};

				return vendorRepository.save(updatedVendor)
						.doOnSuccess(saved -> publishVendorStatusChangedEvent(eventPublisher, reason, saved));
			} catch (ValidationException e) {
				return Mono.error(e);
			}
		});
	}

	private Mono<Vendor> updateAndSaveVendor(Vendor vendor, Vendor.VendorVerificationStatus status) {
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
			log.warn(LOG_VENDOR_EXISTS, email);
			return Mono.error(new VendorAlreadyExistsException("Vendor with email " + email + " already exists"));
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

		existingVendor.setUpdatedAt(LocalDateTime.now());

		return vendorRepository.save(existingVendor)
				.doOnSuccess(vendor -> logVendorUpdated(vendor.getId()));

		//todo publish updateVendorEvent

	}

	private static void logVendorUpdated(UUID id) {
		log.info(LOG_VENDOR_UPDATED, id);
	}

	private static void logUpdatingVendor(UUID id) {
		log.info(LOG_UPDATING_VENDOR, id);
	}
}