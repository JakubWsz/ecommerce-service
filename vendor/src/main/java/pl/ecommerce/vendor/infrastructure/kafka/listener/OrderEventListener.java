package pl.ecommerce.vendor.infrastructure.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.vendor.domain.service.PaymentService;
import pl.ecommerce.vendor.domain.service.VendorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

	private final ObjectMapper objectMapper;
	private final VendorService vendorService;
	private final PaymentService paymentService;

	@KafkaListener(
			topics = "${kafka.topics.order-events:order-events}",
			groupId = "${spring.kafka.consumer.group-id:vendor-service-order-group}"
	)
	public void listenOrderEvents(String message) {
		try {
			DomainEvent event = objectMapper.readValue(message, DomainEvent.class);

			if (event instanceof OrderCompletedEvent orderCompletedEvent) {
				log.info("Received OrderCompletedEvent for order: {}", orderCompletedEvent.getOrderId());
				processOrderCompletedEvent(orderCompletedEvent);
			}
		} catch (Exception e) {
			log.error("Error processing order event: {}", e.getMessage(), e);
		}
	}

	private void processOrderCompletedEvent(OrderCompletedEvent event) {
		log.info("Processing completed order for vendors. Order ID: {}", event.getOrderId());

		// In a real implementation, this would extract vendor-specific order items,
		// calculate commissions, and possibly create automatic payment records

		// For example:
		// event.getItems().stream()
		//     .filter(item -> item.getVendorId() != null)
		//     .collect(Collectors.groupingBy(OrderItem::getVendorId))
		//     .forEach((vendorId, items) -> {
		//         BigDecimal totalAmount = calculateVendorTotal(items);
		//         paymentService.createPayment(vendorId, totalAmount, "USD", "AUTOMATIC");
		//     });
	}
}


