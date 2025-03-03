//package pl.ecommerce.customer.api.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import pl.ecommerce.customer.api.dto.OrderSummaryDto;
//import pl.ecommerce.customer.domain.service.CustomerOrderService;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.UUID;
//
//@Tag(name = "CustomerOrder", description = "Endpoints for customers orders")
//@RestController
//@RequestMapping("/api/v1/customers/orders")
//@RequiredArgsConstructor
//@Slf4j
//public class CustomerOrderController {
//
//	private final CustomerOrderService customerOrderService;
//
//	@GetMapping("/{id}/orders")
//	@Operation(summary = "Get customer order history")
//	public Flux<OrderSummaryDto> getOrderHistory(@PathVariable String id) {
//		return customerOrderService.getOrderHistory(UUID.fromString(id));
//	}
//
//	@GetMapping("/{id}/orders/{orderId}")
//	@Operation(summary = "Get specific order details")
//	public Mono<OrderSummaryDto> getOrderDetails(
//			@PathVariable String id,
//			@PathVariable String orderId) {
//		return customerOrderService.getOrderDetails(UUID.fromString(id), UUID.fromString(orderId));
//	}
//}
