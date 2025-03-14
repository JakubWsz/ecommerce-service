package pl.ecommerce.customer.read.infrastructure.projector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.customer.model.Address;
import pl.ecommerce.commons.customer.model.CustomerStatus;
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
								  ObjectMapper objectMapper, TopicsProvider topicsProvider) {
		super(objectMapper, topicsProvider);
		this.mongoTemplate = mongoTemplate;
		this.customerRepository = customerRepository;
	}

	@EventHandler
	public void on(CustomerRegisteredEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerRegisteredEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		CustomerReadModel customer = buildCustomerReadModel(event, traceId);
		customerRepository.save(customer)
				.doOnSuccess(saved -> log.debug("Customer read model saved successfully: {}, traceId: {}",
						saved.getId(), traceId))
				.doOnError(error -> log.error("Error saving customer read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerUpdatedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildUpdateForEvent(event, traceId);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Updated customer read model: {}, modified: {}, traceId: {}",
						event.getAggregateId(), result.getModifiedCount(), traceId))
				.doOnError(error -> log.error("Error updating customer read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerEmailChangedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerEmailChangedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildEmailChangeUpdate(event, traceId);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Updated customer email in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating customer email in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerEmailVerifiedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerEmailVerifiedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildEmailVerifiedUpdate(event, traceId);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Updated customer email verification in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating customer email verification in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerAddressAddedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerAddressAddedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Address newAddress = buildAddress(event);

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> updateCustomerWithNewAddress(customer, newAddress, event, traceId))
				.map(customerRepository::save)
				.doOnSuccess(updated -> log.debug("Updated customer with new address in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating customer with new address in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerAddressUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerAddressUpdatedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> updateCustomerAddress(customer, event, traceId))
				.map(customerRepository::save)
				.doOnSuccess(updated -> log.debug("Updated address in customer read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating address in customer read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerAddressRemovedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerAddressRemovedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> removeAddressAndUpdateCustomer(customer, event, traceId))
				.map(customerRepository::save)
				.doOnSuccess(updated -> log.debug("Removed address from customer read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error removing address from customer read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerPreferencesUpdatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerPreferencesUpdatedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		customerRepository.findById(event.getAggregateId())
				.flatMap(customer -> {
					updatePreferences(customer, event.getPreferences());
					updateTracingInfo(customer, event, traceId, "UpdatePreferences");
					return customerRepository.save(customer);
				})
				.doOnSuccess(updated -> log.debug("Updated preferences in customer read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error updating preferences in customer read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerDeactivatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerDeactivatedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = buildDeactivationUpdate(event, traceId);

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Deactivated customer in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error deactivating customer in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerReactivatedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerReactivatedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = new Update()
				.set("status", CustomerStatus.ACTIVE)
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null)
				.set("lastOperation", "ReactivateCustomer")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Reactivated customer in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error reactivating customer in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CustomerDeletedEvent event) {
		String traceId = extractTraceId(event);
		log.info("Projecting CustomerDeletedEvent for customer: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));
		Update update = new Update()
				.set("status", CustomerStatus.DELETED)
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null)
				.set("lastOperation", "DeleteCustomer")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.updateFirst(query, update, CustomerReadModel.class)
				.doOnSuccess(result -> log.debug("Marked customer as deleted in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error marking customer as deleted in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}
}
