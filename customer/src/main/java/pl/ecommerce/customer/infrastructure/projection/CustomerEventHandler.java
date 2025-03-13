package pl.ecommerce.customer.infrastructure.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.customer.aplication.dto.AddressDto;
import pl.ecommerce.customer.aplication.dto.CustomerPreferencesDto;
import pl.ecommerce.customer.aplication.dto.CustomerReadModel;
import pl.ecommerce.customer.aplication.dto.PersonalDataDto;
import pl.ecommerce.customer.domain.valueobjects.AddressType;
import pl.ecommerce.customer.domain.valueobjects.CustomerStatus;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Obsługuje zdarzenia domenowe i aktualizuje modele odczytu w MongoDB
 */
@Component
@Slf4j
public class CustomerEventHandler extends DomainEventHandler {

	private final ReactiveMongoTemplate mongoTemplate;

	public CustomerEventHandler(
			ObjectMapper objectMapper,
			KafkaTemplate<String, Object> kafkaTemplate,
			TopicsProvider topicsProvider,
			ReactiveMongoTemplate mongoTemplate) {
		super(objectMapper, kafkaTemplate, topicsProvider);
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * Obsługuje zdarzenie rejestracji klienta
	 */
	@EventHandler
	public void handle(CustomerRegisteredEvent event) {
		log.info("Handling CustomerRegisteredEvent for customer: {}", event.getCustomerId());

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

		mongoTemplate.save(customer)
				.doOnSuccess(saved -> log.debug("Customer read model saved successfully: {}", saved.getId()))
				.doOnError(error -> log.error("Error saving customer read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie aktualizacji klienta
	 */
	@EventHandler
	public void handle(CustomerUpdatedEvent event) {
		log.info("Handling CustomerUpdatedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					// Aktualizuj pola na podstawie przekazanych zmian
					if (event.getChanges().containsKey("firstName")) {
						customer.setFirstName((String) event.getChanges().get("firstName"));
						if (customer.getPersonalData() != null) {
							customer.getPersonalData().setFirstName((String) event.getChanges().get("firstName"));
						}
					}

					if (event.getChanges().containsKey("lastName")) {
						customer.setLastName((String) event.getChanges().get("lastName"));
						if (customer.getPersonalData() != null) {
							customer.getPersonalData().setLastName((String) event.getChanges().get("lastName"));
						}
					}

					if (event.getChanges().containsKey("phoneNumber")) {
						customer.setPhoneNumber((String) event.getChanges().get("phoneNumber"));
						if (customer.getPersonalData() != null) {
							customer.getPersonalData().setPhoneNumber((String) event.getChanges().get("phoneNumber"));
						}
					}

					if (event.getChanges().containsKey("phoneVerified")) {
						customer.setPhoneVerified((Boolean) event.getChanges().get("phoneVerified"));
					}

					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated customer read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error updating customer read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie zmiany emaila
	 */
	@EventHandler
	public void handle(CustomerEmailChangedEvent event) {
		log.info("Handling CustomerEmailChangedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					customer.setEmail(event.getNewEmail());
					customer.setEmailVerified(false);

					if (customer.getPersonalData() != null) {
						customer.getPersonalData().setEmail(event.getNewEmail());
					}

					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated customer email in read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error updating customer email in read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie weryfikacji emaila
	 */
	@EventHandler
	public void handle(CustomerEmailVerifiedEvent event) {
		log.info("Handling CustomerEmailVerifiedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					customer.setEmailVerified(true);
					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated customer email verification in read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error updating customer email verification in read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie weryfikacji numeru telefonu
	 */
	@EventHandler
	public void handle(CustomerPhoneVerifiedEvent event) {
		log.info("Handling CustomerPhoneVerifiedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					customer.setPhoneVerified(true);
					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated customer phone verification in read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error updating customer phone verification in read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie dodania adresu
	 */
	@EventHandler
	public void handle(CustomerAddressAddedEvent event) {
		log.info("Handling CustomerAddressAddedEvent for customer: {}", event.getCustomerId());

		// Przygotuj obiekt adresu
		AddressDto address = new AddressDto();
		address.setId(event.getAddressId());
		address.setStreet(event.getStreet());
		address.setBuildingNumber(event.getBuildingNumber());
		address.setApartmentNumber(event.getApartmentNumber());
		address.setCity(event.getCity());
		address.setPostalCode(event.getPostalCode());
		address.setCountry(event.getCountry());
		address.setState(event.getState());
		address.setDefault(event.isDefault());
		address.setAddressType(AddressType.valueOf(event.getAddressType().name()));

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
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

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated customer with new address in read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error updating customer with new address in read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie aktualizacji adresu
	 */
	@EventHandler
	public void handle(CustomerAddressUpdatedEvent event) {
		log.info("Handling CustomerAddressUpdatedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					if (customer.getAddresses() != null) {
						// Znajdź adres do aktualizacji
						for (AddressDto address : customer.getAddresses()) {
							if (address.getId().equals(event.getAddressId())) {
								address.setStreet(event.getStreet());
								address.setBuildingNumber(event.getBuildingNumber());
								address.setApartmentNumber(event.getApartmentNumber());
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
					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated address in customer read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error updating address in customer read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie usunięcia adresu
	 */
	@EventHandler
	public void handle(CustomerAddressRemovedEvent event) {
		log.info("Handling CustomerAddressRemovedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
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
					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Removed address from customer read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error removing address from customer read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie aktualizacji preferencji
	 */
	@EventHandler
	public void handle(CustomerPreferencesUpdatedEvent event) {
		log.info("Handling CustomerPreferencesUpdatedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
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
					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated preferences in customer read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error updating preferences in customer read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie dezaktywacji klienta
	 */
	@EventHandler
	public void handle(CustomerDeactivatedEvent event) {
		log.info("Handling CustomerDeactivatedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					customer.setStatus(CustomerStatus.INACTIVE);
					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Deactivated customer in read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error deactivating customer in read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie reaktywacji klienta
	 */
	@EventHandler
	public void handle(CustomerReactivatedEvent event) {
		log.info("Handling CustomerReactivatedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					customer.setStatus(CustomerStatus.ACTIVE);
					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Reactivated customer in read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error reactivating customer in read model: {}", error.getMessage(), error))
				.subscribe();
	}

	/**
	 * Obsługuje zdarzenie usunięcia klienta
	 */
	@EventHandler
	public void handle(CustomerDeletedEvent event) {
		log.info("Handling CustomerDeletedEvent for customer: {}", event.getCustomerId());

		mongoTemplate.findById(event.getCustomerId(), CustomerReadModel.class)
				.flatMap(customer -> {
					customer.setStatus(CustomerStatus.DELETED);
					customer.setUpdatedAt(event.getTimestamp());

					return mongoTemplate.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Marked customer as deleted in read model: {}", event.getCustomerId()))
				.doOnError(error -> log.error("Error marking customer as deleted in read model: {}", error.getMessage(), error))
				.subscribe();
	}
}