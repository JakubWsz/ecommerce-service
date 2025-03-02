package pl.ecommerce.customer.domain.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import pl.ecommerce.customer.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerRepository {

	private final ReactiveMongoTemplate mongoTemplate;

	public Mono<Customer> saveCustomer(Customer customer) {
		log.info("Saving customer: {} to Event Store", customer);
		return mongoTemplate.save(customer, "Customer")
				.doOnSuccess(saved -> log.debug("Customer saved successfully: {}", saved))
				.doOnError(e -> log.error("Error saving customer: {}", e.getMessage(), e));
	}

	public Mono<Customer> getCustomerById(UUID id) {
		log.info("Fetching customer with id: {}", id);
		return mongoTemplate.findOne(
						query(where("id").is(id)),
						Customer.class,
						"Customer"
				).doOnNext(customer -> log.debug("Found customer: {}", customer))
				.doOnError(e -> log.error("Error fetching customer with id {}: {}", id, e.getMessage(), e));
	}

	public Mono<Customer> updateCustomer(UUID id, Customer customer) {
		log.info("Updating customer with id: {} with data: {}", id, customer);
		Query query = query(where("id").is(id));
		Update update = new Update()
				.set("active", customer.isActive())
				.set("registrationIp", customer.getRegistrationIp())
				.set("createdAt", customer.getCreatedAt())
				.set("updatedAt", customer.getUpdatedAt())
				.set("lastLoginAt", customer.getLastLoginAt())
				.set("gdprConsent", customer.isGdprConsent())
				.set("consentTimestamp", customer.getConsentTimestamp())
				.set("marketingConsent", customer.isMarketingConsent())
				.set("dataProcessingConsent", customer.isDataProcessingConsent())
				.set("dataRetentionPeriodDays", customer.getDataRetentionPeriodDays())
				.set("personalData", customer.getPersonalData())
				.set("addresses", customer.getAddresses())
				.set("geoLocationData", customer.getGeoLocationData())
				.set("currencies", customer.getCurrencies());

		FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

		return mongoTemplate.findAndModify(query, update, options, Customer.class, "Customer")
				.doOnSuccess(updated -> {
					if (updated != null) {
						log.debug("Customer updated successfully: {}", updated);
					} else {
						log.warn("No customer found to update with id: {}", id);
					}
				})
				.doOnError(e -> log.error("Error updating customer with id {}: {}", id, e.getMessage(), e))
				.switchIfEmpty(Mono.just(customer).doOnNext(c -> log.info("Customer not found, returning input: {}", c)));
	}

	public Mono<Void> deleteCustomer(UUID id) {
		log.info("Deleting customer with id: {}", id);
		Query query = query(where("id").is(id));
		return mongoTemplate.remove(query, Customer.class, "Customer")
				.doOnSuccess(result -> {
					if (result.getDeletedCount() > 0) {
						log.debug("Customer with id {} deleted successfully", id);
					} else {
						log.warn("No customer found to delete with id: {}", id);
					}
				})
				.doOnError(e -> log.error("Error deleting customer with id {}: {}", id, e.getMessage(), e))
				.then();
	}

	public Mono<Customer> getCustomerByEmail(String email) {
		log.info("Fetching customer with email: {}", email);
		Query query = query(where("personalData.email").is(email));
		return mongoTemplate.findOne(query, Customer.class, "Customer")
				.doOnNext(customer -> log.debug("Found customer with email {}: {}", email, customer))
				.doOnError(e -> log.error("Error fetching customer with email {}: {}", email, e.getMessage(), e));
	}

	public Flux<Customer> getAllActiveCustomers() {
		log.info("Fetching all active customers");
		Query query = query(where("active").is(true));
		return mongoTemplate.find(query, Customer.class, "Customer")
				.doOnNext(customer -> log.debug("Found active customer: {}", customer))
				.doOnError(e -> log.error("Error fetching active customers: {}", e.getMessage(), e));
	}
}
