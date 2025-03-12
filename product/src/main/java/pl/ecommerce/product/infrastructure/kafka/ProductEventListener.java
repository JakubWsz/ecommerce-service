package pl.ecommerce.product.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.event.order.OrderCancelledEvent;
import pl.ecommerce.commons.event.order.OrderConfirmedEvent;
import pl.ecommerce.commons.event.order.OrderCreatedEvent;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.product.domain.service.ProductReservationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ProductEventListener extends DomainEventHandler {

	private final ProductReservationService productReservationService;

	public ProductEventListener(ObjectMapper objectMapper, KafkaTemplate<String, Object> kafkaTemplate,
								TopicsProvider topicsProvider, ProductReservationService productReservationService) {
		super(objectMapper, kafkaTemplate, topicsProvider);
		this.productReservationService = productReservationService;
	}

	@Override
	public boolean processEvent(DomainEvent event) {
		try {
			if (event instanceof OrderCreatedEvent orderEvent) {
				log.info("Processing order created event for order: {}", orderEvent.getOrderId());
				handleOrderCreated(orderEvent).block();
				return true;
			} else if (event instanceof OrderConfirmedEvent orderEvent) {
				log.info("Processing order confirmed event for order: {}", orderEvent.getOrderId());
				handleOrderConfirmed(orderEvent).block();
				return true;
			} else if (event instanceof OrderCancelledEvent orderEvent) {
				log.info("Processing order cancelled event for order: {}", orderEvent.getOrderId());
				handleOrderCancelled(orderEvent).block();
				return true;
			}

			return false;
		} catch (Exception e) {
			log.error("Error processing event {}: {}",
					event.getClass().getSimpleName(), e.getMessage(), e);
			return false;
		}
	}

	private Mono<Void> handleOrderCreated(OrderCreatedEvent event) {
		return Flux.fromIterable(event.getItems())
				.flatMap(item -> productReservationService.reserveProduct(
						item.getProductId(), item.getQuantity(), event.getOrderId()))
				.then();
	}

	private Mono<Void> handleOrderConfirmed(OrderConfirmedEvent event) {
		return productReservationService.getReservationsByOrderId(event.getOrderId())
				.flatMap(reservation -> productReservationService.confirmReservation(
						reservation.getProductId(), event.getOrderId()))
				.then();
	}

	private Mono<Void> handleOrderCancelled(OrderCancelledEvent event) {
		return productReservationService.getReservationsByOrderId(event.getOrderId())
				.flatMap(reservation -> productReservationService.cancelReservation(
						reservation.getProductId(), event.getOrderId(), event.getReason()))
				.then();
	}
}