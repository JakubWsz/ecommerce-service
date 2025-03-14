package pl.ecommerce.customer.write.domain.aggregate;

import lombok.Getter;
import pl.ecommerce.commons.customer.model.*;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.customer.write.domain.commands.*;
import pl.ecommerce.customer.write.infrastructure.exception.*;

import java.time.Instant;
import java.util.*;

@Getter
public class CustomerAggregate {
	private UUID id;
	private String email;
	private String firstName;
	private String lastName;
	private PhoneNumber phoneNumber;
	private CustomerStatus status;
	private boolean emailVerified;
	private boolean phoneVerified;
	private Instant createdAt;
	private Instant updatedAt;
	private Address billingAddress;
	private final List<Address> shippingAddresses = new ArrayList<>();
	private UUID defaultShippingAddressId;
	private CustomerPreferences preferences;
	private final List<AuthMethod> authMethods = new ArrayList<>();
	private final Map<String, String> metadata = new HashMap<>();
	private int version = 0;
	private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

	public CustomerAggregate(RegisterCustomerCommand command) {
		if (command.email() == null || command.email().isBlank()) {
			throw new InvalidCustomerDataException("Email is required");
		}
		if (command.firstName() == null || command.firstName().isBlank()) {
			throw new InvalidCustomerDataException("First name is required");
		}
		if (command.lastName() == null || command.lastName().isBlank()) {
			throw new InvalidCustomerDataException("Last name is required");
		}

		applyChange(new CustomerRegisteredEvent(
				command.customerId(),
				command.email(),
				command.firstName(),
				command.lastName(),
				command.phoneNumber() != null ? command.phoneNumber() : null,
				Instant.now(),
				version
		));
	}

	public CustomerAggregate(List<DomainEvent> eventHistory) {
		eventHistory.forEach(this::apply);
	}

	public void updateBasicInfo(UpdateCustomerCommand command) {
		assertCustomerActive();

		Map<String, Object> changes = new HashMap<>();
		boolean hasChanges = false;

		if (command.firstName() != null && !command.firstName().equals(this.firstName)) {
			changes.put("firstName", command.firstName());
			hasChanges = true;
		}

		if (command.lastName() != null && !command.lastName().equals(this.lastName)) {
			changes.put("lastName", command.lastName());
			hasChanges = true;
		}

		if (command.phoneNumber() != null &&
				(this.phoneNumber == null || !command.phoneNumber().equals(this.phoneNumber.value()))) {
			changes.put("phoneNumber", command.phoneNumber());
			changes.put("phoneVerified", false);
			hasChanges = true;
		}

		if (hasChanges) {
			applyChange(new CustomerUpdatedEvent(
					this.id,
					changes,
					Instant.now(),
					version
			));
		}
	}

	public void changeEmail(ChangeCustomerEmailCommand command) {
		assertCustomerActive();

		if (command.newEmail() == null || command.newEmail().isBlank()) {
			throw new InvalidCustomerDataException("New email cannot be empty");
		}

		if (command.newEmail().equals(this.email)) {
			return;
		}

		applyChange(new CustomerEmailChangedEvent(
				this.id,
				this.email,
				command.newEmail(),
				Instant.now(),
				version
		));
	}

	public void verifyEmail(VerifyCustomerEmailCommand command) {
		assertCustomerActive();

		if (this.emailVerified) {
			return;
		}

		applyChange(new CustomerEmailVerifiedEvent(
				this.id,
				this.email,
				Instant.now(),
				version
		));
	}

	public void verifyPhoneNumber(VerifyCustomerPhoneCommand command) {
		assertCustomerActive();

		if (this.phoneVerified || this.phoneNumber == null) {
			return;
		}

		applyChange(new CustomerPhoneVerifiedEvent(
				this.id,
				this.phoneNumber.value(),
				Instant.now(),
				version
		));
	}

	public void addShippingAddress(AddShippingAddressCommand command) {
		assertCustomerActive();

		UUID addressId = command.addressId() != null ? command.addressId() : UUID.randomUUID();

		applyChange(new CustomerAddressAddedEvent(
				this.id,
				addressId,
				command.addressType(),
				command.buildingNumber(),
				command.apartmentNumber(),
				command.street(),
				command.city(),
				command.postalCode(),
				command.country(),
				command.state(),
				command.isDefault(),
				Instant.now(),
				version
		));
	}

	public void updateShippingAddress(UpdateShippingAddressCommand command) {
		assertCustomerActive();

		boolean addressExists = shippingAddresses.stream()
				.anyMatch(address -> address.getId().equals(command.addressId()));

		if (!addressExists) {
			throw new AddressNotFoundException(command.addressId());
		}

		applyChange(new CustomerAddressUpdatedEvent(
				this.id,
				command.addressId(),
				command.buildingNumber(),
				command.apartmentNumber(),
				command.street(),
				command.city(),
				command.postalCode(),
				command.country(),
				command.state(),
				command.isDefault(),
				Instant.now(),
				version
		));
	}

	public void removeShippingAddress(RemoveShippingAddressCommand command) {
		assertCustomerActive();

		boolean addressExists = shippingAddresses.stream()
				.anyMatch(address -> address.getId().equals(command.addressId()));

		if (!addressExists) {
			throw new AddressNotFoundException(command.addressId());
		}

		if (defaultShippingAddressId != null && defaultShippingAddressId.equals(command.addressId())) {
			throw new CannotRemoveDefaultAddressException(command.addressId());
		}

		applyChange(new CustomerAddressRemovedEvent(
				this.id,
				command.addressId(),
				Instant.now(),
				version
		));
	}

	public void updatePreferences(UpdateCustomerPreferencesCommand command) {
		assertCustomerActive();
		applyChange(new CustomerPreferencesUpdatedEvent(
				this.id,
				command.preferences(),
				Instant.now(),
				version
		));
	}

	public void deactivate(DeactivateCustomerCommand command) {
		if (this.status == CustomerStatus.INACTIVE) {
			return;
		}

		applyChange(new CustomerDeactivatedEvent(
				this.id,
				command.reason(),
				Instant.now(),
				version
		));
	}

	public void reactivate(ReactivateCustomerCommand command) {
		if (this.status == CustomerStatus.ACTIVE) {
			return;
		}

		applyChange(new CustomerReactivatedEvent(
				this.id,
				command.note(),
				Instant.now(),
				version
		));
	}

	public void delete(DeleteCustomerCommand command) {
		applyChange(new CustomerDeletedEvent(
				this.id,
				this.email,
				this.firstName,
				this.lastName,
				command.reason(),
				Instant.now(),
				version
		));
	}

	private void assertCustomerActive() {
		if (this.status != CustomerStatus.ACTIVE) {
			throw new CustomerNotActiveException(this.id);
		}
	}

	private void applyChange(DomainEvent event) {
		apply(event);
		uncommittedEvents.add(event);
		version++;
	}

	private void apply(DomainEvent event) {
		if (event instanceof CustomerRegisteredEvent) {
			apply((CustomerRegisteredEvent) event);
		} else if (event instanceof CustomerUpdatedEvent) {
			apply((CustomerUpdatedEvent) event);
		} else if (event instanceof CustomerEmailChangedEvent) {
			apply((CustomerEmailChangedEvent) event);
		} else if (event instanceof CustomerEmailVerifiedEvent) {
			apply((CustomerEmailVerifiedEvent) event);
		} else if (event instanceof CustomerPhoneVerifiedEvent) {
			apply((CustomerPhoneVerifiedEvent) event);
		} else if (event instanceof CustomerAddressAddedEvent) {
			apply((CustomerAddressAddedEvent) event);
		} else if (event instanceof CustomerAddressUpdatedEvent) {
			apply((CustomerAddressUpdatedEvent) event);
		} else if (event instanceof CustomerAddressRemovedEvent) {
			apply((CustomerAddressRemovedEvent) event);
		} else if (event instanceof CustomerPreferencesUpdatedEvent) {
			apply((CustomerPreferencesUpdatedEvent) event);
		} else if (event instanceof CustomerDeactivatedEvent) {
			apply((CustomerDeactivatedEvent) event);
		} else if (event instanceof CustomerReactivatedEvent) {
			apply((CustomerReactivatedEvent) event);
		} else if (event instanceof CustomerDeletedEvent) {
			apply((CustomerDeletedEvent) event);
		}
	}

	private void apply(CustomerRegisteredEvent event) {
		this.id = event.getCustomerId();
		this.email = event.getEmail();
		this.firstName = event.getFirstName();
		this.lastName = event.getLastName();
		this.phoneNumber = event.getPhoneNumber() != null ? new PhoneNumber(event.getPhoneNumber()) : null;
		this.status = CustomerStatus.ACTIVE;
		this.emailVerified = false;
		this.phoneVerified = false;
		this.createdAt = event.getTimestamp();
		this.updatedAt = event.getTimestamp();
		this.preferences = CustomerPreferences.builder().build();
	}

	private void apply(CustomerUpdatedEvent event) {
		Map<String, Object> changes = event.getChanges();

		if (changes.containsKey("firstName")) {
			this.firstName = (String) changes.get("firstName");
		}

		if (changes.containsKey("lastName")) {
			this.lastName = (String) changes.get("lastName");
		}

		if (changes.containsKey("phoneNumber")) {
			this.phoneNumber = new PhoneNumber((String) changes.get("phoneNumber"));
		}

		if (changes.containsKey("phoneVerified")) {
			this.phoneVerified = (Boolean) changes.get("phoneVerified");
		}

		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerEmailChangedEvent event) {
		this.email = event.getNewEmail();
		this.emailVerified = false;
		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerEmailVerifiedEvent event) {
		this.emailVerified = true;
		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerPhoneVerifiedEvent event) {
		this.phoneVerified = true;
		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerAddressAddedEvent event) {
		Address newAddress = new Address(
				event.getAddressId(),
				event.getAddressType(),
				event.getStreet(),
				event.getBuildingNumber(),
				event.getApartmentNumber(),
				event.getCity(),
				event.getState(),
				event.getPostalCode(),
				event.getCountry(),
				event.isDefault()
		);

		if (AddressType.BILLING.equals(event.getAddressType())) {
			this.billingAddress = newAddress;
		} else {
			this.shippingAddresses.add(newAddress);

			if (event.isDefault() || this.defaultShippingAddressId == null) {
				this.defaultShippingAddressId = newAddress.getId();
			}
		}

		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerAddressUpdatedEvent event) {
		if (this.billingAddress != null && this.billingAddress.getId().equals(event.getAddressId())) {
			this.billingAddress = new Address(
					event.getAddressId(),
					AddressType.BILLING,
					event.getStreet(),
					event.getBuildingNumber(),
					event.getApartmentNumber(),
					event.getCity(),
					event.getState(),
					event.getPostalCode(),
					event.getCountry(),
					event.isDefault()
			);
		} else {
			for (int i = 0; i < this.shippingAddresses.size(); i++) {
				if (this.shippingAddresses.get(i).getId().equals(event.getAddressId())) {
					this.shippingAddresses.set(i, new Address(
							event.getAddressId(),
							AddressType.SHIPPING,
							event.getStreet(),
							event.getBuildingNumber(),
							event.getApartmentNumber(),
							event.getCity(),
							event.getState(),
							event.getPostalCode(),
							event.getCountry(),
							event.isDefault()
					));
					break;
				}
			}
		}

		if (event.isDefault()) {
			this.defaultShippingAddressId = event.getAddressId();
		}

		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerAddressRemovedEvent event) {
		this.shippingAddresses.removeIf(address -> address.getId().equals(event.getAddressId()));

		if (this.defaultShippingAddressId != null && this.defaultShippingAddressId.equals(event.getAddressId())) {
			this.defaultShippingAddressId = this.shippingAddresses.isEmpty() ? null : this.shippingAddresses.getFirst().getId();
		}

		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerPreferencesUpdatedEvent event) {
		this.preferences = event.getPreferences();
		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerDeactivatedEvent event) {
		this.status = CustomerStatus.INACTIVE;
		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerReactivatedEvent event) {
		this.status = CustomerStatus.ACTIVE;
		this.updatedAt = event.getTimestamp();
	}

	private void apply(CustomerDeletedEvent event) {
		this.status = CustomerStatus.DELETED;
		this.updatedAt = event.getTimestamp();
	}

	public void clearUncommittedEvents() {
		uncommittedEvents.clear();
	}
}