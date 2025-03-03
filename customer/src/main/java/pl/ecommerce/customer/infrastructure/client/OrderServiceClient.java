package pl.ecommerce.customer.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pl.ecommerce.customer.api.dto.OrderSummaryDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderServiceClient {

	private final WebClient.Builder webClientBuilder;
	@Value("${services.order.url}")
	private String orderServiceBaseUrl;

	public Flux<OrderSummaryDto> getCustomerOrders(UUID customerId) {
		log.info("Fetching orders for customer: {} from Order Service", customerId);

		return webClientBuilder.baseUrl(orderServiceBaseUrl)
				.build()
				.get()
				.uri("/api/orders/customer/{customerId}", customerId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToFlux(OrderSummaryDto.class)
				.doOnError(e -> log.error("Error fetching orders for customer: {}", customerId, e));
	}

	public Mono<OrderSummaryDto> getOrderDetails(UUID customerId, UUID orderId) {
		log.info("Fetching order details for customer: {}, order: {} from Order Service", customerId, orderId);

		return webClientBuilder.baseUrl(orderServiceBaseUrl)
				.build()
				.get()
				.uri("/api/orders/{orderId}", orderId)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(OrderSummaryDto.class)
				.doOnError(e -> log.error("Error fetching order details for orderId: {}", orderId, e));
	}
}