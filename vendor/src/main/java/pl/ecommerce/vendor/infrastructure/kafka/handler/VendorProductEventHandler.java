package pl.ecommerce.vendor.infrastructure.kafka.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.product.ProductCreatedEvent;
import pl.ecommerce.commons.event.product.ProductUpdatedEvent;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
public class VendorProductEventHandler extends DomainEventHandler {

	private final CategoryAssignmentRepository categoryAssignmentRepository;

	public VendorProductEventHandler(ObjectMapper objectMapper,
									 KafkaTemplate<String, Object> kafkaTemplate,
									 TopicsProvider topicsProvider,
									 CategoryAssignmentRepository categoryAssignmentRepository) {
		super(objectMapper, kafkaTemplate, topicsProvider);
		this.categoryAssignmentRepository = categoryAssignmentRepository;
	}

	@EventHandler
	public void handle(ProductCreatedEvent event) {
		log.info("Validating permission for product creation: {}", event.getProductId());

		if (event.getVendorId() != null && event.getCategories() != null && !event.getCategories().isEmpty()) {
			validateCategories(
					event.getVendorId(),
					event.getProductId(),
					event.getCategories(),
					unauthorizedCategories -> notifyUnauthorizedProductCreation(event.getProductId(), event.getVendorId(), unauthorizedCategories),
					"created"
			);
		} else {
			log.info("Skipping validation for product {} - vendorId or categories missing", event.getProductId());
		}
	}

	@EventHandler
	public void handle(ProductUpdatedEvent event) {
		log.info("Validating permission for product update: {}", event.getProductId());

		if (event.getVendorId() != null &&
				event.getCategories() != null &&
				event.getChangedFields() != null &&
				event.getChangedFields().containsKey("categories")) {

			validateCategories(
					event.getVendorId(),
					event.getProductId(),
					event.getCategories(),
					unauthorizedCategories -> notifyUnauthorizedProductUpdate(event.getProductId(), event.getVendorId(), unauthorizedCategories),
					"updated"
			);
		} else {
			log.info("Skipping validation for product update {} - no category changes or missing data", event.getProductId());
		}
	}

	private void validateCategories(UUID vendorId, UUID productId, List<UUID> categories,
									Consumer<List<UUID>> notifyUnauthorized, String action) {
		List<UUID> unauthorizedCategories = new ArrayList<>();

		Flux.fromIterable(categories)
				.flatMap(categoryId ->
						categoryAssignmentRepository.existsByVendorIdAndCategoryId(vendorId, categoryId)
								.map(exists -> isExists(vendorId, action,
										categoryId, exists, unauthorizedCategories))
				)
				.collectList()
				.subscribe(
						results -> processResults(productId, notifyUnauthorized, action, unauthorizedCategories),
						error -> log.error("Error validating product categories: {}", error.getMessage(), error)
				);
	}

	private static void processResults(UUID productId, Consumer<List<UUID>> notifyUnauthorized, String action, List<UUID> unauthorizedCategories) {
		if (!unauthorizedCategories.isEmpty()) {
			log.error("Product {} {} with unauthorized categories: {}",
					productId, action, unauthorizedCategories);
			notifyUnauthorized.accept(unauthorizedCategories);
		} else {
			log.info("Product {} validation successful - all categories authorized", productId);
		}
	}

	private static Boolean isExists(UUID vendorId, String action, UUID categoryId, Boolean exists, List<UUID> unauthorizedCategories) {
		if (!exists) {
			log.warn("Vendor {} {} product with unauthorized category {}",
					vendorId, action, categoryId);
			unauthorizedCategories.add(categoryId);
		}
		return exists;
	}

	private void notifyUnauthorizedProductCreation(UUID productId, UUID vendorId, List<UUID> unauthorizedCategories) {
		log.info("Notification sent for unauthorized product creation: Product ID: {}, Vendor ID: {}, Unauthorized Categories: {}",
				productId, vendorId, unauthorizedCategories);
	}

	private void notifyUnauthorizedProductUpdate(UUID productId, UUID vendorId, List<UUID> unauthorizedCategories) {
		log.info("Notification sent for unauthorized product update: Product ID: {}, Vendor ID: {}, Unauthorized Categories: {}",
				productId, vendorId, unauthorizedCategories);
	}
}
