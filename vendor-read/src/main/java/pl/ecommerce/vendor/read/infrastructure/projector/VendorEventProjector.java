package pl.ecommerce.vendor.read.infrastructure.projector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.vendor.*;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.commons.model.vendor.VendorStatus;
import pl.ecommerce.vendor.read.domain.VendorReadModel;
import pl.ecommerce.vendor.read.infrastructure.repository.VendorReadRepository;

import java.time.Instant;

@Component
@Slf4j
public class VendorEventProjector extends DomainEventHandler {

	private final ReactiveMongoTemplate mongoTemplate;
	private final VendorReadRepository vendorRepository;
	private final VendorEventProjectorHelper projectorHelper;

	public VendorEventProjector(ReactiveMongoTemplate mongoTemplate,
								VendorReadRepository vendorRepository,
								VendorEventProjectorHelper projectorHelper,
								ObjectMapper objectMapper,
								TopicsProvider topicsProvider) {
		super(objectMapper, topicsProvider);
		this.mongoTemplate = mongoTemplate;
		this.vendorRepository = vendorRepository;
		this.projectorHelper = projectorHelper;
	}

	@EventHandler
	public void on(VendorRegisteredEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorRegisteredEvent for vendor: {}, traceId: {}",
				event.getVendorId(), traceId);

		projectorHelper.createVendorFromRegistrationEvent(event)
				.flatMap(vendorRepository::save)
				.doOnSuccess(savedVendor ->
						log.debug("Vendor read model created: {}, traceId: {}",
								savedVendor.getId(), traceId))
				.doOnError(error ->
						log.error("Error creating vendor read model: {}, traceId: {}",
								error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(VendorUpdatedEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorUpdatedEvent for vendor: {}, traceId: {}",
				event.getVendorId(), traceId);

		vendorRepository.findById(event.getVendorId())
				.flatMap(vendor -> projectorHelper.applyVendorUpdates(vendor, event))
				.flatMap(vendorRepository::save)
				.doOnSuccess(savedVendor ->
						log.debug("Vendor read model updated: {}, traceId: {}",
								savedVendor.getId(), traceId))
				.doOnError(error ->
						log.error("Error updating vendor read model: {}, traceId: {}",
								error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(VendorVerificationCompletedEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorVerificationCompletedEvent for vendor: {}, traceId: {}",
				event.getVendorId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getVendorId()));
		Update update = new Update()
				.set("verified", VendorStatus.APPROVED.equals(event.getVerificationStatus()))
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId",event.extractSpanId())
				.set("lastOperation", "VerifyVendor")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.findAndModify(
						Query.query(Criteria.where("_id").is(event.getVendorId())
								.and("status").is(VendorStatus.PENDING.name())),
						update.set("status", VendorStatus.ACTIVE.name()),
						VendorReadModel.class
				)
				.switchIfEmpty(
						mongoTemplate.updateFirst(query, update, VendorReadModel.class)
								.flatMap(updateResult -> mongoTemplate.findOne(query, VendorReadModel.class))
				)
				.doOnSuccess(result -> log.debug("Updated vendor verification status: {}, traceId: {}",
						event.getVendorId(), traceId))
				.doOnError(error -> log.error("Error updating vendor verification status: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}


	@EventHandler
	public void on(VendorStatusChangedEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorStatusChangedEvent for vendor: {}, traceId: {}",
				event.getVendorId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getVendorId()));
		Update update = new Update()
				.set("status", event.getNewStatus().name())
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", event.extractSpanId())
				.set("lastOperation", "ChangeVendorStatus")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.updateFirst(query, update, VendorReadModel.class)
				.doOnSuccess(result -> log.debug("Updated vendor status to {}: {}, traceId: {}",
						event.getNewStatus(), event.getVendorId(), traceId))
				.doOnError(error -> log.error("Error updating vendor status: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(VendorCategoryAssignedEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorCategoryAssignedEvent for vendor: {}, category: {}, traceId: {}",
				event.getVendorId(), event.getCategoryId(), traceId);

		vendorRepository.findById(event.getVendorId())
				.flatMap(vendor -> projectorHelper.addCategory(vendor, event))
				.flatMap(vendorRepository::save)
				.doOnSuccess(savedVendor ->
						log.debug("Category added to vendor: {}, category: {}, traceId: {}",
								savedVendor.getId(), event.getCategoryId(), traceId))
				.doOnError(error ->
						log.error("Error adding category to vendor: {}, traceId: {}",
								error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(VendorCategoryRemovedEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorCategoryRemovedEvent for vendor: {}, category: {}, traceId: {}",
				event.getVendorId(), event.getCategoryId(), traceId);

		vendorRepository.findById(event.getVendorId())
				.flatMap(vendor -> projectorHelper.removeCategory(vendor, event))
				.flatMap(vendorRepository::save)
				.doOnSuccess(savedVendor ->
						log.debug("Category removed from vendor: {}, category: {}, traceId: {}",
								savedVendor.getId(), event.getCategoryId(), traceId))
				.doOnError(error ->
						log.error("Error removing category from vendor: {}, traceId: {}",
								error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(VendorBankDetailsUpdatedEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorBankDetailsUpdatedEvent for vendor: {}, traceId: {}",
				event.getVendorId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getVendorId()));
		Update update = new Update()
				.set("bankDetails.accountNumber", event.getBankAccountNumber())
				.set("bankDetails.bankName", event.getBankName())
				.set("bankDetails.swiftCode", event.getBankSwiftCode())
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", event.extractSpanId())
				.set("lastOperation", "UpdateBankDetails")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.updateFirst(query, update, VendorReadModel.class)
				.doOnSuccess(result -> log.debug("Updated bank details for vendor: {}, traceId: {}",
						event.getVendorId(), traceId))
				.doOnError(error -> log.error("Error updating bank details: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(VendorDeletedEvent event) {
		String traceId = event.extractTraceId();
		log.info("Projecting VendorDeletedEvent for vendor: {}, traceId: {}",
				event.getVendorId(), traceId);

		Query query = Query.query(Criteria.where("_id").is(event.getVendorId()));
		Update update = new Update()
				.set("status", VendorStatus.DELETED.name())
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", event.extractSpanId())
				.set("lastOperation", "DeleteVendor")
				.set("lastUpdatedAt", Instant.now());

		mongoTemplate.updateFirst(query, update, VendorReadModel.class)
				.doOnSuccess(result -> log.debug("Marked vendor as deleted: {}, traceId: {}",
						event.getVendorId(), traceId))
				.doOnError(error -> log.error("Error marking vendor as deleted: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}
}