package pl.ecommerce.product.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.event.order.OrderCancelledEvent;
import pl.ecommerce.commons.event.order.OrderConfirmedEvent;
import pl.ecommerce.commons.event.order.OrderCreatedEvent;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.product.domain.service.ProductReservationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class OrderEventConsumer extends DomainEventHandler {

	private final ProductReservationService productReservationService;

	public OrderEventConsumer(
			ObjectMapper objectMapper,
			KafkaTemplate<String, Object> kafkaTemplate,
			TopicsProvider topicsProvider,
			ProductReservationService productReservationService) {
		super(objectMapper, kafkaTemplate, topicsProvider);
		this.productReservationService = productReservationService;
	}

	@EventHandler
	public void handle(OrderCreatedEvent event) {
		log.info("Processing OrderCreatedEvent for order: {}", event.getOrderId());

		Flux.fromIterable(event.getItems())
				.flatMap(item -> productReservationService.reserveProduct(
						item.getProductId(), item.getQuantity(), event.getOrderId()))
				.then()
				.doOnSuccess(v -> log.info("Successfully processed OrderCreatedEvent for order: {}", event.getOrderId()))
				.onErrorResume(e -> {
					log.error("Error processing OrderCreatedEvent: {}", e.getMessage(), e);
					return Mono.empty();
				})
				.subscribe();
	}

	@EventHandler
	public void handle(OrderConfirmedEvent event) {
		log.info("Processing OrderConfirmedEvent for order: {}", event.getOrderId());

		productReservationService.getReservationsByOrderId(event.getOrderId())
				.flatMap(reservation -> productReservationService.confirmReservation(
						reservation.getProductId(), event.getOrderId()))
				.then()
				.doOnSuccess(v -> log.info("Successfully processed OrderConfirmedEvent for order: {}", event.getOrderId()))
				.onErrorResume(e -> {
					log.error("Error processing OrderConfirmedEvent: {}", e.getMessage(), e);
					return Mono.empty();
				})
				.subscribe();
	}

	@EventHandler
	public void handle(OrderCancelledEvent event) {
		log.info("Processing OrderCancelledEvent for order: {}", event.getOrderId());

		productReservationService.getReservationsByOrderId(event.getOrderId())
				.flatMap(reservation -> productReservationService.cancelReservation(
						reservation.getProductId(), event.getOrderId(), event.getReason()))
				.then()
				.doOnSuccess(v -> log.info("Successfully processed OrderCancelledEvent for order: {}", event.getOrderId()))
				.onErrorResume(e -> {
					log.error("Error processing OrderCancelledEvent: {}", e.getMessage(), e);
					return Mono.empty();
				})
				.subscribe();
	}
}
