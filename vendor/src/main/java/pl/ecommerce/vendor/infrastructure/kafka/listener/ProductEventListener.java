package pl.ecommerce.vendor.infrastructure.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.product.ProductCreatedEvent;
import pl.ecommerce.commons.event.product.ProductUpdatedEvent;
import pl.ecommerce.vendor.domain.repository.CategoryAssignmentRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

	private final ObjectMapper objectMapper;
	private final CategoryAssignmentRepository categoryAssignmentRepository;

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
		log.info("Validating permission for product creation: {}", event.getProductId());

		if (event.getVendorId() != null && event.getCategories() != null && !event.getCategories().isEmpty()) {
			UUID vendorId = UUID.fromString(event.getVendorId());
			List<UUID> unauthorizedCategories = new ArrayList<>();

			Flux.fromIterable(event.getCategories())
					.flatMap(categoryId ->
							categoryAssignmentRepository.existsByVendorIdAndCategoryId(vendorId, categoryId)
									.flatMap(exists -> {
										if (!exists) {
											log.warn("Vendor {} created product in unauthorized category {}",
													event.getVendorId(), categoryId);
											unauthorizedCategories.add(categoryId);
											return Mono.just(false);
										}
										return Mono.just(true);
									})
					)
					.collectList()
					.subscribe(
							results -> {
								if (!unauthorizedCategories.isEmpty()) {
									log.error("Product {} created with unauthorized categories: {}",
											event.getProductId(), unauthorizedCategories);
									notifyUnauthorizedProductCreation(event.getProductId(), vendorId, unauthorizedCategories);
								} else {
									log.info("Product {} validation successful - all categories authorized", event.getProductId());
								}
							},
							error -> log.error("Error validating product categories: {}", error.getMessage(), error)
					);
		} else {
			log.info("Skipping validation for product {} - vendorId or categories missing", event.getProductId());
		}
	}

	private void validateProductUpdate(ProductUpdatedEvent event) {
		log.info("Validating permission for product update: {}", event.getProductId());

		if (event.getVendorId() != null && event.getCategories() != null &&
				event.getChangedFields() != null && event.getChangedFields().containsKey("categories")) {

			UUID vendorId = UUID.fromString(event.getVendorId());
			List<UUID> unauthorizedCategories = new ArrayList<>();

			Flux.fromIterable(event.getCategories())
					.flatMap(categoryId ->
							categoryAssignmentRepository.existsByVendorIdAndCategoryId(vendorId, categoryId)
									.flatMap(exists -> {
										if (!exists) {
											log.warn("Vendor {} updated product with unauthorized category {}",
													event.getVendorId(), categoryId);
											unauthorizedCategories.add(categoryId);
											return Mono.just(false);
										}
										return Mono.just(true);
									})
					)
					.collectList()
					.subscribe(
							results -> {
								if (!unauthorizedCategories.isEmpty()) {
									log.error("Product {} updated with unauthorized categories: {}",
											event.getProductId(), unauthorizedCategories);
									// Tutaj można wysłać powiadomienie lub wywołać API do oznaczenia produktu
									notifyUnauthorizedProductUpdate(event.getProductId(), vendorId, unauthorizedCategories);
								} else {
									log.info("Product {} update validation successful - all categories authorized", event.getProductId());
								}
							},
							error -> log.error("Error validating updated product categories: {}", error.getMessage(), error)
					);
		} else {
			log.info("Skipping validation for product update {} - no category changes or missing data", event.getProductId());
		}
	}

	private void notifyUnauthorizedProductCreation(UUID productId, UUID vendorId, List<UUID> unauthorizedCategories) {
		// W rzeczywistej implementacji, tutaj byłby kod do:
		// 1. Wysłania powiadomienia do administratora
		// 2. Oznaczenia produktu jako wymagającego przeglądu
		// 3. Ewentualnie automatycznego usunięcia nieautoryzowanych kategorii

		log.info("Notification sent for unauthorized product creation: Product ID: {}, Vendor ID: {}, Unauthorized Categories: {}",
				productId, vendorId, unauthorizedCategories);

		// Przykład: można wysłać nowe zdarzenie informujące o naruszeniu
		// eventPublisher.publish(new UnauthorizedProductCategoriesEvent(productId, vendorId, unauthorizedCategories));
	}

	private void notifyUnauthorizedProductUpdate(UUID productId, UUID vendorId, List<UUID> unauthorizedCategories) {
		// Podobnie jak powyżej, ale dla aktualizacji produktu

		log.info("Notification sent for unauthorized product update: Product ID: {}, Vendor ID: {}, Unauthorized Categories: {}",
				productId, vendorId, unauthorizedCategories);

		// Przykład: można wysłać nowe zdarzenie informujące o naruszeniu
		// eventPublisher.publish(new UnauthorizedProductUpdateEvent(productId, vendorId, unauthorizedCategories));
	}
}
