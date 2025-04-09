package pl.ecommerce.customer.read.infrastructure.projector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.model.customer.Address;
import pl.ecommerce.commons.model.customer.CustomerStatus;
import pl.ecommerce.commons.event.customer.*;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.customer.read.domain.model.CustomerReadModel;
import pl.ecommerce.customer.read.infrastructure.repository.CustomerReadRepository;

import java.time.Instant;

import static pl.ecommerce.customer.read.infrastructure.projector.CustomerEventProjectorHelper.*;

@Component
@Slf4j
public class CustomerEventProjector extends DomainEventHandler {

	private final ReactiveMongoTemplate mongoTemplate;
	private final CustomerReadRepository customerRepository;

	public CustomerEventProjector(ReactiveMongoTemplate mongoTemplate, CustomerReadRepository customerRepository,
								  ObjectMapper objectMapper, TopicsProvider topicsProvider, Environment environment) {
		super(objectMapper, topicsProvider,environment.getProperty("spring.application.name"));
		this.mongoTemplate = mongoTemplate;
		this.customerRepository = customerRepository;
	}

	@EventHandler
	public void on(CustomerRegisteredEvent event) {
		log.info("Projecting CustomerRegisteredEvent for customer: {}",
				event.getAggregateId());

		CustomerReadModel customer = buildCustomerReadModel(event);
		customerRepository.save(customer)
				.doOnSuccess(saved -> log.debug("Customer read model saved successfully: {}",
						saved.getId()))
				.doOnError(error -> log.error("Error saving customer read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerUpdatedEvent event) {
		log.info("Projecting CustomerUpdatedEvent for customer: {}",
				event.getAggregateId());

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildUpdateForEvent(event);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Updated customer read model: {}, modified: {}",
						event.getAggregateId(), result.getModifiedCount()))
				.doOnError(error -> log.error("Error updating customer read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerEmailChangedEvent event) {
		log.info("Projecting CustomerEmailChangedEvent for customer: {}",
				event.getAggregateId());

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildEmailChangeUpdate(event);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Updated customer email in read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error updating customer email in read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerEmailVerifiedEvent event) {
		log.info("Projecting CustomerEmailVerifiedEvent for customer: {}",
				event.getAggregateId());

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildEmailVerifiedUpdate(event);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Updated customer email verification in read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error updating customer email verification in read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerAddressAddedEvent event) {
		log.info("Projecting CustomerAddressAddedEvent for customer: {}",
				event.getAggregateId());

		Address newAddress = buildAddress(event);

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> updateCustomerWithNewAddress(customer, newAddress, event))
				.flatMap(customerRepository::save)
				.doOnSuccess(updated -> log.debug("Updated customer with new address in read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error updating customer with new address in read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerAddressUpdatedEvent event) {
		log.info("Projecting CustomerAddressUpdatedEvent for customer: {}",
				event.getAggregateId());

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> updateCustomerAddress(customer, event))
				.flatMap(customerRepository::save)
				.doOnSuccess(updated -> log.debug("Updated address in customer read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error updating address in customer read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerAddressRemovedEvent event) {
		log.info("Projecting CustomerAddressRemovedEvent for customer: {}",
				event.getAggregateId());

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> removeAddressAndUpdateCustomer(customer, event))
				.flatMap(customerRepository::save)
				.doOnSuccess(updated -> log.debug("Removed address from customer read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error removing address from customer read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerPreferencesUpdatedEvent event) {
		log.info("Projecting CustomerPreferencesUpdatedEvent for customer: {}",
				event.getAggregateId());

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> {
					updatePreferences(customer, event.getPreferences());
					updateTracingInfo(customer, event, "UpdatePreferences");
					return customerRepository.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated preferences in customer read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error updating preferences in customer read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerDeactivatedEvent event) {
		log.info("Projecting CustomerDeactivatedEvent for customer: {}",
				event.getAggregateId());

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildDeactivationUpdate(event);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Deactivated customer in read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error deactivating customer in read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerReactivatedEvent event) {
		
		log.info("Projecting CustomerReactivatedEvent for customer: {}",
				event.getAggregateId());

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = new Update()
				.set("status", CustomerStatus.ACTIVE)
				.set("updatedAt", event.getTimestamp())
				.set("lastOperation", "ReactivateCustomer")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Reactivated customer in read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error reactivating customer in read model: {}",
						error.getMessage(), error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerDeletedEvent event) {
		
		log.info("Projecting CustomerDeletedEvent for customer: {}",
				event.getAggregateId());

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = new Update()
				.set("status", CustomerStatus.DELETED)
				.set("updatedAt", event.getTimestamp())
				.set("lastOperation", "DeleteCustomer")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Marked customer as deleted in read model: {}",
						event.getAggregateId()))
				.doOnError(error -> log.error("Error marking customer as deleted in read model: {}",
						error.getMessage(), error))
				.subscribe();
	}
}