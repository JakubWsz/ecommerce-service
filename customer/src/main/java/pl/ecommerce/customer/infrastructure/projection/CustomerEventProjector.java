package pl.ecommerce.customer.infrastructure.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.customer.aplication.dto.AddressDto;
import pl.ecommerce.customer.aplication.dto.CustomerPreferencesDto;
import pl.ecommerce.customer.aplication.dto.CustomerReadModel;
import pl.ecommerce.customer.aplication.dto.PersonalDataDto;
import pl.ecommerce.customer.domain.valueobjects.AddressType;
import pl.ecommerce.customer.domain.valueobjects.CustomerStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Klasa odpowiedzialna za obsługę zdarzeń i aktualizację modelu odczytu w MongoDB
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventProjector {

	private final ReactiveMongoTemplate mongoTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Obsługuje zdarzenie rejestracji klienta
	 */
	@EventHandler
	public void on(CustomerRegisteredEvent event) {
		log.info("Projecting CustomerRegisteredEvent for customer: {}", event.getCustomerId());

		CustomerReadModel customer = new CustomerReadModel();
		customer.setId(event.getCustomerId());
		customer.setEmail(event.getEmail());
		customer.setFirstName(event.getFirstName());
		customer.setLastName(event.getLastName());
		customer.setPhoneNumber(event.getPhoneNumber());
		customer.setEmailVerified(false);
		customer.setPhoneVerified(false);
		customer.setStatus(CustomerStatus.ACTIVE);
		customer.setCreatedAt(event.getTimestamp());
		customer.setUpdatedAt(event.getTimestamp());
		customer.setAddresses(new ArrayList<>());
		customer.setMetadata(new HashMap<>());

		// Utwórz DTO danych personalnych
		PersonalDataDto personalData = new PersonalDataDto();
		personalData.setEmail(event.getEmail());
		personalData.setFirstName(event.getFirstName());
		personalData.setLastName(event.getLastName());
		personalData.setPhoneNumber(event.getPhoneNumber());
		customer.setPersonalData(personalData);

		// Utwórz domyślne preferencje
		CustomerPreferencesDto preferences = new CustomerPreferencesDto();
		preferences.setPreferredLanguage("pl");
		preferences.setPreferredCurrency("PLN");
		preferences.setMarketingConsent(false);
		preferences.setNewsletterSubscribed(false);
		customer.setPreferences(preferences);

		mongoTemplate.save(customer, "customers").subscribe(
				saved -> log.debug("Customer read model saved successfully: {}", saved.getId()),
				error -> log.error("Error saving customer read model: {}", error.getMessage(), error)
		);
	}

	/**
	 * Obsługuje zdarzenie aktualizacji klienta
	 */
	@EventHandler
	public void on(CustomerUpdatedEvent event) {
		log.info("Projecting CustomerUpdatedEvent for customer: {}", event.getCustomerId());

		Query query = Query.query(Criteria.where("_id").is(event.getCustomerId()));
		Update update = new Update().set("updatedAt", event.getTimestamp());

		// Aktualizuj pola na podstawie przekazanych zmian
		Map<String, Object> changes = event.getChanges();
		if (changes.containsKey("firstName")) {
			update.set("firstName", changes.get("firstName"));
			update.set("personalData.firstName", changes.get("firstName"));
		}

		if (changes.containsKey("lastName")) {
			update.set("lastName", changes.get("lastName"));
			update.set("personalData.lastName", changes.get("lastName"));
		}

		if (changes.containsKey("phoneNumber")) {
			update.set("phoneNumber", changes.get("phoneNumber"));
			update.set("personalData.phoneNumber", changes.get("phoneNumber"));
		}

		if (changes.containsKey("phoneVerified")) {
			update.set("phoneVerified", changes.get("phoneVerified"));
		}

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class, "customers").subscribe(
				result -> log.debug("Updated customer read model: {}, modified: {}",
						event.getCustomerId(), result.getModifiedCount()),
				error -> log.error("Error updating customer read model: {}", error.getMessage(), error)
		);
	}

	/**
	 * Obsługuje zdarzenie zmiany emaila
	 */
	@EventHandler
	public void on(CustomerEmailChangedEvent event) {
		log.info("Projecting CustomerEmailChangedEvent for customer: {}", event.getCustomerId());

		Query query = Query.query(Criteria.where("_id").is(event.getCustomerId()));
		Update update = new Update()
				.set("email", event.getNewEmail())
				.set("personalData.email", event.getNewEmail())
				.set("emailVerified", false)
				.set("updatedAt", event.getTimestamp());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class, "customers").subscribe(
				result -> log.debug("Updated customer email in read model: {}", event.getCustomerId()),
				error -> log.error("Error updating customer email in read model: {}", error.getMessage(), error)
		);
	}

	/**
	 * Obsługuje zdarzenie weryfikacji emaila
	 */
	@EventHandler
	public void on(CustomerEmailVerifiedEvent event) {
		log.info("Projecting CustomerEmailVerifiedEvent for customer: {}", event.getCustomerId());

		Query query = Query.query(Criteria.where("_id").is(event.getCustomerId()));
		Update update = new Update()
				.set("emailVerified", true)
				.set("updatedAt", event.getTimestamp());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class, "customers").subscribe(
				result -> log.debug("Updated customer email verification in read model: {}", event.getCustomerId()),
				error -> log.error("Error updating customer email verification in read model: {}", error.getMessage(), error)
		);
	}

	/**
	 * Obsługuje zdarzenie weryfikacji numeru telefonu
	 */
	@EventHandler
	public void on(CustomerPhoneVerifiedEvent event) {
		log.info("Projecting CustomerPhoneVerifiedEvent for customer: {}", event.getCustomerId());

		Query query = Query.query(Criteria.where("_id").is(event.getCustomerId()));
		Update update = new Update()
				.set("phoneVerified", true)
				.set("updatedAt", event.getTimestamp());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class, "customers").subscribe(
				result -> log.debug("Updated customer phone verification in read model: {}", event.getCustomerId()),
				error -> log.error("Error updating customer phone verification in read model: {}", error.getMessage(), error)
		);
	}

	/**
	 * Obsługuje zdarzenie dodania adresu
	 */
	@EventHandler
	public void on(CustomerAddressAddedEvent event) {
		log.info("Projecting CustomerAddressAddedEvent for customer: {}", event.getCustomerId());

		// Przygotuj obiekt adresu
		AddressDto address = new AddressDto();
		address.setId(event.getAddressId());
		address.setStreet(event.getStreet());
		address.setCity(event.getCity());
		address.setPostalCode(event.getPostalCode());
		address.setCountry(event.getCountry());
		address.setState(event.getState());
		address.setDefault(event.isDefault());
		address.setAddressType(AddressType.valueOf(event.getAddressType().name()));

		// Znajdź istniejący model odczytu klienta
		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class, "customers")
				.flatMap(customer -> {
					// Jeśli adres ma być domyślny, usuń flagę domyślności z innych adresów
					if (event.isDefault() && customer.getAddresses() != null) {
						for (AddressDto existingAddress : customer.getAddresses()) {
							if (existingAddress.getAddressType().name().equals(event.getAddressType().name())) {
								existingAddress.setDefault(false);
							}
						}
					}

					// Dodaj nowy adres
					if (customer.getAddresses() == null) {
						customer.setAddresses(new ArrayList<>());
					}
					customer.getAddresses().add(address);
					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer, "customers");
				})
				.subscribe(
						result -> log.debug("Updated customer with new address in read model: {}", event.getCustomerId()),
						error -> log.error("Error updating customer with new address in read model: {}", error.getMessage(), error)
				);
	}

	/**
	 * Obsługuje zdarzenie aktualizacji adresu
	 */
	@EventHandler
	public void on(CustomerAddressUpdatedEvent event) {
		log.info("Projecting CustomerAddressUpdatedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class, "customers")
				.flatMap(customer -> {
					if (customer.getAddresses() != null) {
						// Znajdź adres do aktualizacji
						for (AddressDto address : customer.getAddresses()) {
							if (address.getId().equals(event.getAddressId())) {
								address.setStreet(event.getStreet());
								address.setCity(event.getCity());
								address.setPostalCode(event.getPostalCode());
								address.setCountry(event.getCountry());
								address.setState(event.getState());

								// Jeśli adres ma być domyślny, usuń flagę domyślności z innych adresów
								if (event.isDefault()) {
									for (AddressDto otherAddress : customer.getAddresses()) {
										if (!otherAddress.getId().equals(event.getAddressId()) &&
												otherAddress.getAddressType() == address.getAddressType()) {
											otherAddress.setDefault(false);
										}
									}
									address.setDefault(true);
								}
								break;
							}
						}
					}

					customer.setUpdatedAt(event.getTimestamp());
					return mongoTemplate.save(customer, "customers");
				})
				.subscribe(
						result -> log.debug("Updated address in customer read model: {}", event.getCustomerId()),
						error -> log.error("Error updating address in customer read model: {}", error.getMessage(), error)
				);
	}

	/**
	 * Obsługuje zdarzenie usunięcia adresu
	 */
	@EventHandler
	public void on(CustomerAddressRemovedEvent event) {
		log.info("Projecting CustomerAddressRemovedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class, "customers")
				.flatMap(customer -> {
					if (customer.getAddresses() != null) {
						// Znajdź indeks adresu do usunięcia
						int indexToRemove = -1;
						for (int i = 0; i < customer.getAddresses().size(); i++) {
							if (customer.getAddresses().get(i).getId().equals(event.getAddressId())) {
								indexToRemove = i;
								break;
							}
						}

						// Jeśli znaleziono adres, usuń go
						if (indexToRemove != -1) {
							AddressDto removedAddress = customer.getAddresses().remove(indexToRemove);

							// Jeśli usunięty adres był domyślny, ustaw pierwszy adres jako domyślny
							if (removedAddress.isDefault() && !customer.getAddresses().isEmpty()) {
								// Znajdź pierwszy adres tego samego typu
								for (AddressDto address : customer.getAddresses()) {
									if (address.getAddressType() == removedAddress.getAddressType()) {
										address.setDefault(true);
										break;
									}
								}
							}
						}
					}

					customer.setUpdatedAt(event.getTimestamp());
					return mongoTemplate.save(customer, "customers");
				})
				.subscribe(
						result -> log.debug("Removed address from customer read model: {}", event.getCustomerId()),
						error -> log.error("Error removing address from customer read model: {}", error.getMessage(), error)
				);
	}

	/**
	 * Obsługuje zdarzenie aktualizacji preferencji
	 */
	@EventHandler
	public void on(CustomerPreferencesUpdatedEvent event) {
		log.info("Projecting CustomerPreferencesUpdatedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class, "customers")
				.flatMap(customer -> {
					CustomerPreferencesDto preferences = customer.getPreferences();
					if (preferences == null) {
						preferences = new CustomerPreferencesDto();
						customer.setPreferences(preferences);
					}

					// Mapuj preferencje z domeny na DTO
					preferences.setMarketingConsent(event.getPreferences().marketingConsent());
					preferences.setNewsletterSubscribed(event.getPreferences().newsletterSubscribed());
					preferences.setPreferredLanguage(event.getPreferences().preferredLanguage());
					preferences.setPreferredCurrency(event.getPreferences().preferredCurrency());
					preferences.setFavoriteCategories(event.getPreferences().favoriteCategories());

					customer.setUpdatedAt(event.getTimestamp());
					return mongoTemplate.save(customer, "customers");
				})
				.subscribe(
						result -> log.debug("Updated preferences in customer read model: {}", event.getCustomerId()),
						error -> log.error("Error updating preferences in customer read model: {}", error.getMessage(), error)
				);
	}

	/**
	 * Obsługuje zdarzenie dezaktywacji klienta
	 */
	@EventHandler
	public void on(CustomerDeactivatedEvent event) {
		log.info("Projecting CustomerDeactivatedEvent for customer: {}", event.getCustomerId());

		Query query = Query.query(Criteria.where("_id").is(event.getCustomerId()));
		Update update = new Update()
				.set("status", CustomerStatus.INACTIVE)
				.set("updatedAt", event.getTimestamp());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class, "customers")
				.subscribe(
						result -> log.debug("Deactivated customer in read model: {}", event.getCustomerId()),
						error -> log.error("Error deactivating customer in read model: {}", error.getMessage(), error)
				);
	}

	/**
	 * Obsługuje zdarzenie reaktywacji klienta
	 */
	@EventHandler
	public void on(CustomerReactivatedEvent event) {
		log.info("Projecting CustomerReactivatedEvent for customer: {}", event.getCustomerId());

		Query query = Query.query(Criteria.where("_id").is(event.getCustomerId()));
		Update update = new Update()
				.set("status", CustomerStatus.ACTIVE)
				.set("updatedAt", event.getTimestamp());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class, "customers")
				.subscribe(
						result -> log.debug("Reactivated customer in read model: {}", event.getCustomerId()),
						error -> log.error("Error reactivating customer in read model: {}", error.getMessage(), error)
				);
	}

	/**
	 * Obsługuje zdarzenie usunięcia klienta
	 */
	@EventHandler
	public void on(CustomerDeletedEvent event) {
		log.info("Projecting CustomerDeletedEvent for customer: {}", event.getCustomerId());

		Query query = Query.query(Criteria.where("_id").is(event.getCustomerId()));
		Update update = new Update()
				.set("status", CustomerStatus.DELETED)
				.set("updatedAt", event.getTimestamp());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class, "customers")
				.subscribe(
						result -> log.debug("Marked customer as deleted in read model: {}", event.getCustomerId()),
						error -> log.error("Error marking customer as deleted in read model: {}", error.getMessage(), error)
				);
	}
}
