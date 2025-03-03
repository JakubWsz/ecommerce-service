package pl.ecommerce.customer.domain.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.ecommerce.customer.api.dto.OrderSummaryDto;
import pl.ecommerce.customer.domain.repository.CustomerRepository;
import pl.ecommerce.customer.infrastructure.client.OrderServiceClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerOrderService {

	private final CustomerRepository customerRepository;
	private final OrderServiceClient orderServiceClient;

	@CircuitBreaker(name = "orderService", fallbackMethod = "getOrderHistoryFallback")
	@Retry(name = "orderService")
	public Flux<OrderSummaryDto> getOrderHistory(UUID customerId) {
		log.info("Getting order history for customer: {}", customerId);

		return customerRepository.getCustomerById(customerId)
				.switchIfEmpty(Mono.error(new RuntimeException("Customer not found: " + customerId)))
				.flatMapMany(customer -> orderServiceClient.getCustomerOrders(customerId));
	}

	public Flux<OrderSummaryDto> getOrderHistoryFallback(UUID customerId, Throwable t) {
		log.error("Fallback for getOrderHistory. Error: {}", t.getMessage());
		return Flux.empty();
	}

	@CircuitBreaker(name = "orderService", fallbackMethod = "getOrderDetailsFallback")
	@Retry(name = "orderService")
	public Mono<OrderSummaryDto> getOrderDetails(UUID customerId, UUID orderId) {
		log.info("Getting order details for customer: {}, order: {}", customerId, orderId);

		return customerRepository.getCustomerById(customerId)
				.switchIfEmpty(Mono.error(new RuntimeException("Customer not found: " + customerId)))
				.flatMap(customer -> orderServiceClient.getOrderDetails(customerId, orderId));
	}

	public Mono<OrderSummaryDto> getOrderDetailsFallback(String customerId, String orderId, Throwable t) {
		log.error("Fallback for getOrderDetails. Error: {}", t.getMessage());
		return Mono.empty();
	}
}