package pl.ecommerce.customer.write.domain.aggregate;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.EventApplier;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.commons.model.customer.*;
import pl.ecommerce.customer.write.domain.commands.*;
import pl.ecommerce.customer.write.domain.handler.*;
import pl.ecommerce.customer.write.infrastructure.exception.*;

import java.time.Instant;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
@Slf4j
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

	private final Map<Class<? extends DomainEvent>, EventApplier> eventAppliers = new HashMap<>();
	private final Map<Class<?>, CommandHandler<?>> commandHandlers = new HashMap<>();

	public CustomerAggregate(RegisterCustomerCommand command) {
		initializeEventAppliers();
		initializeCommandHandlers();
		executeCommand(command);
	}

	public CustomerAggregate(List<DomainEvent> eventHistory) {
		initializeEventAppliers();
		initializeCommandHandlers();
		eventHistory.forEach(this::apply);
	}

	public void updateBasicInfo(UpdateCustomerCommand command) {
		executeCommand(command);
	}

	public void changeEmail(ChangeCustomerEmailCommand command) {
		executeCommand(command);
	}

	public void verifyEmail(VerifyCustomerEmailCommand command) {
		executeCommand(command);
	}

	public void verifyPhoneNumber(VerifyCustomerPhoneCommand command) {
		executeCommand(command);
	}

	public void addShippingAddress(AddShippingAddressCommand command) {
		executeCommand(command);
	}

	public void updateShippingAddress(UpdateShippingAddressCommand command) {
		executeCommand(command);
	}

	public void removeShippingAddress(RemoveShippingAddressCommand command) {
		executeCommand(command);
	}

	public void updatePreferences(UpdateCustomerPreferencesCommand command) {
		executeCommand(command);
	}

	public void deactivate(DeactivateCustomerCommand command) {
		executeCommand(command);
	}

	public void reactivate(ReactivateCustomerCommand command) {
		executeCommand(command);
	}

	public void delete(DeleteCustomerCommand command) {
		executeCommand(command);
	}

	public void clearUncommittedEvents() {
		uncommittedEvents.clear();
	}

	protected void applyChange(DomainEvent event) {
		boolean needsTraceInfo = isNull(event.getTraceId()) || isNull(event.getSpanId());

		if (needsTraceInfo) {
			trySetTracingContextFromCurrentSpan(event);
		}

		apply(event);
		uncommittedEvents.add(event);
		version++;
	}

	protected void apply(DomainEvent event) {
		EventApplier applier = eventAppliers.get(event.getClass());
		if (nonNull(applier)) {
			applier.apply(event);
		}
	}

	private void trySetTracingContextFromCurrentSpan(DomainEvent event) {
		Span currentSpan = Span.current();
		SpanContext context = currentSpan.getSpanContext();

		if (context.isValid()) {
			if (isNull(event.getTraceId())) {
				event.setTraceId(context.getTraceId());
			}
			if (isNull(event.getSpanId())) {
				event.setSpanId(context.getSpanId());
			}
		} else {
			log.warn("No valid OpenTelemetry SpanContext found. Could not set tracing context for event: {} (Type: {})",
					event.getEventId(), event.getEventType());
		}
	}

	private <T extends Command> void executeCommand(T command) {
		@SuppressWarnings("unchecked")
		CommandHandler<T> handler = (CommandHandler<T>) commandHandlers.get(command.getClass());
		if (nonNull(handler)) {
			handler.handle(command);
		}
	}

	private void assertCustomerActive() {
		if (this.status != CustomerStatus.ACTIVE) {
			throw new CustomerNotActiveException(this.id);
		}
	}

	private void initializeEventAppliers() {
		eventAppliers.put(CustomerRegisteredEvent.class, event -> {
			CustomerRegisteredEvent e = (CustomerRegisteredEvent) event;
			this.id = e.getCustomerId();
			this.email = e.getEmail();
			this.firstName = e.getFirstName();
			this.lastName = e.getLastName();
			this.phoneNumber = nonNull(e.getPhoneNumber()) ? new PhoneNumber(e.getPhoneNumber()) : null;
			this.status = CustomerStatus.ACTIVE;
			this.emailVerified = false;
			this.phoneVerified = false;
			this.createdAt = e.getEventTimestamp();
			this.updatedAt = e.getEventTimestamp();
			this.preferences = CustomerPreferences.builder().build();
		});

		eventAppliers.put(CustomerUpdatedEvent.class, event -> {
			CustomerUpdatedEvent e = (CustomerUpdatedEvent) event;
			Map<String, Object> changes = e.getChanges();

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

			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(CustomerEmailChangedEvent.class, event -> {
			CustomerEmailChangedEvent e = (CustomerEmailChangedEvent) event;
			this.email = e.getNewEmail();
			this.emailVerified = false;
			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(CustomerEmailVerifiedEvent.class, event -> {
			this.emailVerified = true;
			this.updatedAt = event.getTimestamp();
		});

		eventAppliers.put(CustomerPhoneVerifiedEvent.class, event -> {
			this.phoneVerified = true;
			this.updatedAt = event.getTimestamp();
		});

		eventAppliers.put(CustomerAddressAddedEvent.class, event -> {
			CustomerAddressAddedEvent e = (CustomerAddressAddedEvent) event;
			Address newAddress = new Address(
					e.getAddressId(),
					e.getAddressType(),
					e.getStreet(),
					e.getBuildingNumber(),
					e.getApartmentNumber(),
					e.getCity(),
					e.getVoivodeship(),
					e.getPostalCode(),
					e.getCountry(),
					e.isDefault()
			);

			if (AddressType.BILLING.equals(e.getAddressType())) {
				this.billingAddress = newAddress;
			} else {
				this.shippingAddresses.add(newAddress);

				if (e.isDefault() || isNull(this.defaultShippingAddressId)) {
					this.defaultShippingAddressId = newAddress.getId();
				}
			}

			this.updatedAt = e.getEventTimestamp();
		});

		eventAppliers.put(CustomerAddressUpdatedEvent.class, event -> {
			CustomerAddressUpdatedEvent e = (CustomerAddressUpdatedEvent) event;
			if (nonNull(this.billingAddress) && this.billingAddress.getId().equals(e.getAddressId())) {
				this.billingAddress = new Address(
						e.getAddressId(),
						AddressType.BILLING,
						e.getStreet(),
						e.getBuildingNumber(),
						e.getApartmentNumber(),
						e.getCity(),
						e.getVoivodeship(),
						e.getPostalCode(),
						e.getCountry(),
						e.isDefault()
				);
			} else {
				for (int i = 0; i < this.shippingAddresses.size(); i++) {
					if (this.shippingAddresses.get(i).getId().equals(e.getAddressId())) {
						this.shippingAddresses.set(i, new Address(
								e.getAddressId(),
								AddressType.SHIPPING,
								e.getStreet(),
								e.getBuildingNumber(),
								e.getApartmentNumber(),
								e.getCity(),
								e.getVoivodeship(),
								e.getPostalCode(),
								e.getCountry(),
								e.isDefault()
						));
						break;
					}
				}
			}

			if (e.isDefault()) {
				this.defaultShippingAddressId = e.getAddressId();
			}

			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(CustomerAddressRemovedEvent.class, event -> {
			CustomerAddressRemovedEvent e = (CustomerAddressRemovedEvent) event;
			this.shippingAddresses.removeIf(address -> address.getId().equals(e.getAddressId()));

			if (nonNull(this.defaultShippingAddressId) && this.defaultShippingAddressId.equals(e.getAddressId())) {
				this.defaultShippingAddressId = this.shippingAddresses.isEmpty() ? null : this.shippingAddresses.getFirst().getId();
			}

			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(CustomerPreferencesUpdatedEvent.class, event -> {
			CustomerPreferencesUpdatedEvent e = (CustomerPreferencesUpdatedEvent) event;
			this.preferences = e.getPreferences();
			this.updatedAt = e.getEventTimestamp();
		});

		eventAppliers.put(CustomerDeactivatedEvent.class, event -> {
			this.status = CustomerStatus.INACTIVE;
			this.updatedAt = event.getTimestamp();
		});

		eventAppliers.put(CustomerReactivatedEvent.class, event -> {
			this.status = CustomerStatus.ACTIVE;
			this.updatedAt = event.getTimestamp();
		});

		eventAppliers.put(CustomerDeletedEvent.class, event -> {
			this.status = CustomerStatus.DELETED;
			this.updatedAt = event.getTimestamp();
		});
	}

	private void initializeCommandHandlers() {
		commandHandlers.put(RegisterCustomerCommand.class, new RegisterCustomerCommandHandler(this));
		commandHandlers.put(UpdateCustomerCommand.class, new UpdateCustomerCommandHandler(this));
		commandHandlers.put(ChangeCustomerEmailCommand.class, new ChangeCustomerEmailCommandHandler(this));
		commandHandlers.put(VerifyCustomerEmailCommand.class, new VerifyCustomerEmailCommandHandler(this));
		commandHandlers.put(VerifyCustomerPhoneCommand.class, new VerifyCustomerPhoneCommandHandler(this));
		commandHandlers.put(AddShippingAddressCommand.class, new AddShippingAddressCommandHandler(this));
		commandHandlers.put(UpdateShippingAddressCommand.class, new UpdateShippingAddressCommandHandler(this));
		commandHandlers.put(RemoveShippingAddressCommand.class, new RemoveShippingAddressCommandHandler(this));
		commandHandlers.put(UpdateCustomerPreferencesCommand.class, new UpdateCustomerPreferencesCommandHandler(this));
		commandHandlers.put(DeactivateCustomerCommand.class, new DeactivateCustomerCommandHandler(this));
		commandHandlers.put(ReactivateCustomerCommand.class, new ReactivateCustomerCommandHandler(this));
		commandHandlers.put(DeleteCustomerCommand.class, new DeleteCustomerCommandHandler(this));
	}

	public interface AggregateHelper {
		void applyChange(DomainEvent event);
		void assertCustomerActive();
	}

	public AggregateHelper getHelper() {
		return new AggregateHelper() {
			@Override
			public void applyChange(DomainEvent event) {
				CustomerAggregate.this.applyChange(event);
			}

			@Override
			public void assertCustomerActive() {
				CustomerAggregate.this.assertCustomerActive();
			}
		};
	}
}