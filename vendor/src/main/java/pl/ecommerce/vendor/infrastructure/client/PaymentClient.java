package pl.ecommerce.vendor.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pl.ecommerce.vendor.api.dto.PaymentRequest;
import pl.ecommerce.vendor.api.dto.PaymentResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${payment.service.url}")
	private String paymentGatewayUrl;

	@CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
	public Mono<PaymentResponse> processPayment(PaymentRequest request) {
		log.info("Processing payment for vendor {} with amount {}", request.vendorId(), request.amount());

		return webClientBuilder.baseUrl(paymentGatewayUrl)
				.build()
				.post()
				.uri("/api/v1/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(PaymentResponse.class)
				.doOnSuccess(response -> log.info("Payment processed successfully: {}", response))
				.doOnError(e -> log.error("Error processing payment: {}", e.getMessage(), e));
	}

	public Mono<PaymentResponse> processPaymentFallback(PaymentRequest request, Exception e) {
		log.error("Payment processing failed: {}", e.getMessage(), e);

		LocalDateTime now = LocalDateTime.now();

		return Mono.just(
				PaymentResponse.builder()
						.id(UUID.randomUUID())
						.vendorId(request.vendorId())
						.amount(request.amount())
						.status("FAILED")
						.paymentMethod(request.paymentMethod())
						.referenceId(UUID.randomUUID())
						.notes("Payment processing unavailable: " + e.getMessage())
						.paymentDate(now)
						.createdAt(now)
						.updatedAt(now)
						.build()
		);
	}
}
