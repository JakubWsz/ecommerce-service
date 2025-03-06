package pl.ecommerce.vendor.infrastructure.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;

/**
 * Kafka listener for product-related events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

	private final ObjectMapper objectMapper;
	private final CategoryAssignmentRepository categoryAssignmentRepository;

	/**
	 * Listen for product-related events to validate vendor permissions
	 */
	@KafkaListener(
			topics = "${kafka.topics.product-events:product-events}",
			groupId = "${spring.kafka.consumer.group-id:vendor-service-product-group}"
	)
	public void listenProductEvents(String message) {
		try {
			DomainEvent event = objectMapper.readValue(message, DomainEvent.class);

			if (event instanceof ProductCreatedEvent productCreatedEvent) {
				log.info("Received ProductCreatedEvent for product: {}", productCreatedEvent.getProductId());
				validateProductCreation(productCreatedEvent);
			} else if (event instanceof ProductUpdatedEvent productUpdatedEvent) {
				log.info("Received ProductUpdatedEvent for product: {}", productUpdatedEvent.getProductId());
				validateProductUpdate(productUpdatedEvent);
			}
		} catch (Exception e) {
			log.error("Error processing product event: {}", e.getMessage(), e);
		}
	}

	private void validateProductCreation(ProductCreatedEvent event) {
		// In a real implementation, this would check if the vendor has permissions
		// for the categories and possibly reject or flag the product if not

		// For example:
		// if (event.getVendorId() != null && event.getCategories() != null) {
		//     event.getCategories().forEach(categoryId -> {
		//         categoryAssignmentRepository.existsByVendorIdAndCategoryId(
		//             UUID.fromString(event.getVendorId()), categoryId)
		//             .subscribe(exists -> {
		//                 if (!exists) {
		//                     log.warn("Vendor {} created product in unauthorized category {}",
		//                           event.getVendorId(), categoryId);
		//                     // Could send an alert or notification here
		//                 }
		//             });
		//     });
		// }
	}

	private void validateProductUpdate(ProductUpdatedEvent event) {
		// Similar to validateProductCreation but for updates
		log.info("Validating permission for product update: {}", event.getProductId());
	}
}
