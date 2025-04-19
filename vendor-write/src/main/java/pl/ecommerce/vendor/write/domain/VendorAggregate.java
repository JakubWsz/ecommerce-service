package pl.ecommerce.vendor.write.domain;

import lombok.Getter;
import pl.ecommerce.commons.command.Command;
import pl.ecommerce.commons.command.CommandHandler;
import pl.ecommerce.commons.event.AbstractDomainEvent;
import pl.ecommerce.commons.event.EventApplier;
import pl.ecommerce.commons.event.vendor.*;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.vendor.write.domain.command.*;
import pl.ecommerce.vendor.write.domain.handler.*;
import pl.ecommerce.vendor.write.infrastructure.exception.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Getter
public class VendorAggregate {
	private UUID id;
	private String name;
	private String businessName;
	private String taxId;
	private String email;
	private String phone;
	private String legalForm;
	private VendorStatus status;
	private boolean verified;
	private final Set<UUID> assignedCategories = new HashSet<>();
	private BigDecimal commissionRate;
	private String bankAccountNumber;
	private String bankName;
	private String bankSwiftCode;
	private String contactPersonName;
	private String contactPersonEmail;
	private Instant createdAt;
	private Instant updatedAt;
	private int version = 0;

	private final List<AbstractDomainEvent> uncommittedEvents = new ArrayList<>();
	private final Map<Class<? extends AbstractDomainEvent>, EventApplier> eventAppliers = new HashMap<>();
	private final Map<Class<? extends Command>, CommandHandler<? extends Command>> commandHandlers = new HashMap<>();

	public VendorAggregate(RegisterVendorCommand command) {
		initializeEventAppliers();
		initializeCommandHandlers();
		executeCommand(command);
	}

	public VendorAggregate(List<AbstractDomainEvent> eventHistory) {
		initializeEventAppliers();
		initializeCommandHandlers();
		eventHistory.forEach(this::apply);
	}

	public void updateVendor(UpdateVendorCommand command) {
		executeCommand(command);
	}

	public void verifyVendor(VerifyVendorCommand command) {
		executeCommand(command);
	}

	public void changeStatus(ChangeVendorStatusCommand command) {
		executeCommand(command);
	}

	public void addCategory(AddCategoryCommand command) {
		executeCommand(command);
	}

	public void removeCategory(RemoveCategoryCommand command) {
		executeCommand(command);
	}

	public void updateBankDetails(UpdateBankDetailsCommand command) {
		executeCommand(command);
	}

	public void delete(DeleteVendorCommand command) {
		executeCommand(command);
	}

	public void clearUncommittedEvents() {
		uncommittedEvents.clear();
	}

	protected void applyChange(AbstractDomainEvent event) {
		apply(event);
		uncommittedEvents.add(event);
		version++;
	}

	protected void apply(AbstractDomainEvent event) {
		EventApplier applier = eventAppliers.get(event.getClass());
		if (applier != null) {
			applier.apply(event);
		}
	}

	private <T extends Command> void executeCommand(T command) {
		@SuppressWarnings("unchecked")
		CommandHandler<T> handler = (CommandHandler<T>) commandHandlers.get(command.getClass());
		if (handler != null) {
			handler.handle(command);
		}
	}

	private void assertVendorActive() {
		if (!VendorStatus.ACTIVE.equals(this.status)) {
			throw new VendorNotActiveException(this.id);
		}
	}

	private void initializeEventAppliers() {
		eventAppliers.put(VendorRegisteredEvent.class, event -> {
			VendorRegisteredEvent e = (VendorRegisteredEvent) event;
			this.id = e.getVendorId();
			this.name = e.getName();
			this.businessName = e.getBusinessName();
			this.taxId = e.getTaxId();
			this.email = e.getEmail();
			this.phone = e.getPhone();
			this.legalForm = e.getLegalForm();
			this.status = e.getStatus();
			this.verified = false;
			this.commissionRate = e.getCommissionRate();
			if (e.getInitialCategories() != null) {
				this.assignedCategories.addAll(e.getInitialCategories());
			}
			this.createdAt = e.getTimestamp();
			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(VendorUpdatedEvent.class, event -> {
			VendorUpdatedEvent e = (VendorUpdatedEvent) event;
			Map<String, Object> changes = e.getChanges();

			if (changes.containsKey("name")) {
				this.name = (String) changes.get("name");
			}

			if (changes.containsKey("businessName")) {
				this.businessName = (String) changes.get("businessName");
			}

			if (changes.containsKey("phone")) {
				this.phone = (String) changes.get("phone");
			}

			if (changes.containsKey("contactPersonName")) {
				this.contactPersonName = (String) changes.get("contactPersonName");
			}

			if (changes.containsKey("contactPersonEmail")) {
				this.contactPersonEmail = (String) changes.get("contactPersonEmail");
			}

			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(VendorVerificationCompletedEvent.class, event -> {
			VendorVerificationCompletedEvent e = (VendorVerificationCompletedEvent) event;
			this.verified = VendorStatus.APPROVED.equals(e.getVerificationStatus());
			if (this.verified && VendorStatus.PENDING.equals(this.status)) {
				this.status = VendorStatus.ACTIVE;
			}
			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(VendorStatusChangedEvent.class, event -> {
			VendorStatusChangedEvent e = (VendorStatusChangedEvent) event;
			this.status = e.getNewStatus();
			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(VendorCategoryAssignedEvent.class, event -> {
			VendorCategoryAssignedEvent e = (VendorCategoryAssignedEvent) event;
			this.assignedCategories.add(e.getCategoryId());
			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(VendorCategoryRemovedEvent.class, event -> {
			VendorCategoryRemovedEvent e = (VendorCategoryRemovedEvent) event;
			this.assignedCategories.remove(e.getCategoryId());
			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(VendorBankDetailsUpdatedEvent.class, event -> {
			VendorBankDetailsUpdatedEvent e = (VendorBankDetailsUpdatedEvent) event;
			this.bankAccountNumber = e.getBankAccountNumber();
			this.bankName = e.getBankName();
			this.bankSwiftCode = e.getBankSwiftCode();
			this.updatedAt = e.getTimestamp();
		});

		eventAppliers.put(VendorDeletedEvent.class, event -> {
			this.status = VendorStatus.DELETED;
			this.updatedAt = event.getTimestamp();
		});
	}

	private void initializeCommandHandlers() {
		commandHandlers.put(RegisterVendorCommand.class, new RegisterVendorCommandHandler(this));
		commandHandlers.put(UpdateVendorCommand.class, new UpdateVendorCommandHandler(this));
		commandHandlers.put(VerifyVendorCommand.class, new VerifyVendorCommandHandler(this));
		commandHandlers.put(ChangeVendorStatusCommand.class, new ChangeVendorStatusCommandHandler(this));
		commandHandlers.put(AddCategoryCommand.class, new AddCategoryCommandHandler(this));
		commandHandlers.put(RemoveCategoryCommand.class, new RemoveCategoryCommandHandler(this));
		commandHandlers.put(UpdateBankDetailsCommand.class, new UpdateBankDetailsCommandHandler(this));
		commandHandlers.put(DeleteVendorCommand.class, new DeleteVendorCommandHandler(this));
	}

	public interface AggregateHelper {
		void applyChange(AbstractDomainEvent event);
		void assertVendorActive();
	}

	public AggregateHelper getHelper() {
		return new AggregateHelper() {
			@Override
			public void applyChange(AbstractDomainEvent event) {
				VendorAggregate.this.applyChange(event);
			}

			@Override
			public void assertVendorActive() {
				VendorAggregate.this.assertVendorActive();
			}
		};
	}
}