package pl.ecommerce.customer.read.infrastructure.projector;

import org.springframework.data.mongodb.core.query.Update;
import pl.ecommerce.commons.model.customer.Address;
import pl.ecommerce.commons.model.customer.AddressType;
import pl.ecommerce.commons.model.customer.CustomerPreferences;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;
import pl.ecommerce.customer.read.domain.model.PersonalData;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public interface CustomerEventProjectorHelper {

	static CustomerReadModel buildCustomerReadModel(CustomerRegisteredEvent event, String traceId, String spanId) {
		var aggregateId = event.getAggregateId();

		return CustomerReadModel.builder()
				.id(aggregateId)
				.email(event.getEmail())
				.firstName(event.getFirstName())
				.lastName(event.getLastName())
				.phoneNumber(event.getPhoneNumber())
				.emailVerified(false)
				.phoneVerified(false)
				.status(CustomerStatus.ACTIVE)
				.createdAt(event.getTimestamp())
				.updatedAt(event.getTimestamp())
				.addresses(new ArrayList<>())
				.metadata(new HashMap<>())
				.lastTraceId(traceId)
				.lastSpanId(spanId)
				.lastOperation("RegisterCustomer")
				.lastUpdatedAt(Instant.now())
				.personalData(buildPersonalData(event))
				.preferences(buildDefaultPreferences())
				.build();
	}

	static PersonalData buildPersonalData(CustomerRegisteredEvent event) {
		return PersonalData.builder()
				.email(event.getEmail())
				.firstName(event.getFirstName())
				.lastName(event.getLastName())
				.phoneNumber(event.getPhoneNumber())
				.build();
	}

	static CustomerPreferences buildDefaultPreferences() {
		return CustomerPreferences.builder()
				.preferredLanguage("pl")
				.preferredCurrency("PLN")
				.marketingConsent(false)
				.newsletterSubscribed(false)
				.build();
	}

	static Update buildUpdateForEvent(CustomerUpdatedEvent event, String traceId, String spanId) {

		Update update = new Update()
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", spanId)
				.set("lastOperation", "UpdateCustomer")
				.set("lastUpdatedAt", Instant.now());
		event.getChanges().forEach((key, value) -> applyChange(update, key, value));
		return update;
	}

	static void applyChange(Update update, String key, Object value) {
		if ("firstName".equals(key)) {
			update.set("firstName", value);
			update.set("personalData.firstName", value);
		} else if ("lastName".equals(key)) {
			update.set("lastName", value);
			update.set("personalData.lastName", value);
		} else if ("phoneNumber".equals(key)) {
			update.set("phoneNumber", value);
			update.set("personalData.phoneNumber", value);
		} else if ("phoneVerified".equals(key)) {
			update.set("phoneVerified", value);
		}
	}

	static Update buildEmailChangeUpdate(CustomerEmailChangedEvent event, String traceId, String spanId) {

		return new Update()
				.set("email", event.getNewEmail())
				.set("personalData.email", event.getNewEmail())
				.set("emailVerified", false)
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", spanId)
				.set("lastOperation", "ChangeEmail")
				.set("lastUpdatedAt", Instant.now());
	}

	static Update buildEmailVerifiedUpdate(CustomerEmailVerifiedEvent event, String traceId, String spanId) {

		return new Update()
				.set("emailVerified", true)
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", spanId)
				.set("lastOperation", "VerifyEmail")
				.set("lastUpdatedAt", Instant.now());
	}

	static Address buildAddress(CustomerAddressAddedEvent event) {
		return Address.builder()
				.id(event.getAddressId())
				.street(event.getStreet())
				.buildingNumber(event.getBuildingNumber())
				.apartmentNumber(event.getApartmentNumber())
				.city(event.getCity())
				.postalCode(event.getPostalCode())
				.country(event.getCountry())
				.voivodeship(event.getVoivodeship())
				.isDefault(event.isDefault())
				.addressType(AddressType.valueOf(event.getAddressType().name()))
				.build();
	}

	static Mono<CustomerReadModel> updateCustomerWithNewAddress(CustomerReadModel customer, Address newAddress,
																CustomerAddressAddedEvent event,
																String traceId, String spanId) {
		if (event.isDefault() && nonNull(customer.getAddresses())) {
			customer.getAddresses().forEach(existingAddress -> {
				if (existingAddress.getAddressType().equals(newAddress.getAddressType())) {
					existingAddress.setDefault(false);
				}
			});
		}
		if (isNull(customer.getAddresses())) {
			customer.setAddresses(new ArrayList<>());
		}
		customer.getAddresses().add(newAddress);
		updateTracingInfo(customer, event, traceId, "AddAddress", spanId);
		return Mono.just(customer);
	}

	static Mono<CustomerReadModel> updateCustomerAddress(CustomerReadModel customer,
														 CustomerAddressUpdatedEvent event,
														 String traceId, String spanId) {
		if (nonNull(customer.getAddresses())) {
			for (Address address : customer.getAddresses()) {
				if (address.getId().equals(event.getAddressId())) {
					updateAddressFields(address, event);
					if (event.isDefault()) {
						setDefaultForAddress(customer, address);
					}
					break;
				}
			}
		}
		updateTracingInfo(customer, event, traceId, "UpdateAddress", spanId);
		return Mono.just(customer);
	}

	static void updateAddressFields(Address address, CustomerAddressUpdatedEvent event) {
		address.setStreet(event.getStreet());
		address.setBuildingNumber(event.getBuildingNumber());
		address.setApartmentNumber(event.getApartmentNumber());
		address.setCity(event.getCity());
		address.setPostalCode(event.getPostalCode());
		address.setCountry(event.getCountry());
		address.setVoivodeship(event.getVoivodeship());
	}

	static void setDefaultForAddress(CustomerReadModel customer, Address targetAddress) {
		AddressType targetType = targetAddress.getAddressType();
		customer.getAddresses().forEach(otherAddress -> {
			if (!otherAddress.getId().equals(targetAddress.getId()) &&
					otherAddress.getAddressType().equals(targetType)) {
				otherAddress.setDefault(false);
			}
		});
		targetAddress.setDefault(true);
	}

	static Mono<CustomerReadModel> removeAddressAndUpdateCustomer(CustomerReadModel customer,
																  CustomerAddressRemovedEvent event, String traceId
			, String spanId) {
		if (nonNull(customer.getAddresses())) {
			Address addressToRemove = customer.getAddresses().stream()
					.filter(address -> address.getId().equals(event.getAddressId()))
					.findFirst()
					.orElse(null);
			if (nonNull(addressToRemove)) {
				boolean wasDefault = addressToRemove.isDefault();
				AddressType addressType = addressToRemove.getAddressType();
				customer.getAddresses().remove(addressToRemove);
				if (wasDefault && !customer.getAddresses().isEmpty()) {
					customer.getAddresses().stream()
							.filter(address -> address.getAddressType().equals(addressType))
							.findFirst()
							.ifPresent(address -> address.setDefault(true));
				}
			}
		}
		updateCustomerTracingInfo(customer, event.getTimestamp(), traceId, "RemoveAddress", spanId);
		return Mono.just(customer);
	}

	static void updateCustomerTracingInfo(CustomerReadModel customer, Instant timestamp,
										  String traceId, String operation, String spanId) {

		customer.setUpdatedAt(timestamp);
		customer.setLastTraceId(traceId);
		customer.setLastSpanId(spanId);
		customer.setLastOperation(operation);
		customer.setLastUpdatedAt(Instant.now());
	}

	static void updatePreferences(CustomerReadModel customer, CustomerPreferences newPreferences) {
		CustomerPreferences preferences = customer.getPreferences();
		if (preferences == null) {
			preferences = CustomerPreferences.builder().build();
			customer.setPreferences(preferences);
		}
		preferences.setMarketingConsent(newPreferences.isMarketingConsent());
		preferences.setNewsletterSubscribed(newPreferences.isNewsletterSubscribed());
		preferences.setPreferredLanguage(newPreferences.getPreferredLanguage());
		preferences.setPreferredCurrency(newPreferences.getPreferredCurrency());
		preferences.setFavoriteCategories(newPreferences.getFavoriteCategories());
	}

	static Update buildDeactivationUpdate(CustomerDeactivatedEvent event, String traceId, String spanId) {
		return new Update()
				.set("status", CustomerStatus.INACTIVE)
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", spanId)
				.set("lastOperation", "DeactivateCustomer")
				.set("lastUpdatedAt", Instant.now());
	}

	static void updateTracingInfo(CustomerReadModel customer, CustomerEvent event, String traceId,
								  String operation, String spanId) {

		customer.setUpdatedAt(event.getTimestamp());
		customer.setLastTraceId(traceId);
		customer.setLastSpanId(spanId);
		customer.setLastOperation(operation);
		customer.setLastUpdatedAt(Instant.now());
	}
}
