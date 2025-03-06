package pl.ecommerce.vendor.infrastructure.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.customer.CustomerRegisteredEvent;
import pl.ecommerce.vendor.domain.service.VendorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventListener {

	private final ObjectMapper objectMapper;
	private final VendorService vendorService;

	@KafkaListener(
			topics = "${kafka.topics.customer-events:customer-registered.event,customer.updated.event,customer.deleted.event}",
			groupId = "${spring.kafka.consumer.group-id:vendor-service-customer-group}"
	)
	public void listenCustomerEvents(String message) {
		try {
			DomainEvent event = objectMapper.readValue(message, DomainEvent.class);

			if (event instanceof CustomerRegisteredEvent customerRegisteredEvent) {
				log.info("Received CustomerRegisteredEvent for customer: {}", customerRegisteredEvent.getCustomerId());
			}
		} catch (Exception e) {
			log.error("Error processing customer event: {}", e.getMessage(), e);
		}
	}
}