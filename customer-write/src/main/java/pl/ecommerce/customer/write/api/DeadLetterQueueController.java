//package pl.ecommerce.customer.write.api;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import pl.ecommerce.commons.tracing.TracedOperation;
//import pl.ecommerce.customer.write.infrastructure.event.DlqRepository;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//
//@RestController
//@RequestMapping("/api/admin/dlq")
//@RequiredArgsConstructor
//public class DeadLetterQueueController {
//
//	private final DlqRepository dlqRepositoryService;
//
//	@GetMapping("/summary")
//	@TracedOperation("getDlqSummary")
//	public ResponseEntity<Map<String, Object>> getDlqSummary() {
//		Map<String, Integer> countByStatus = dlqRepositoryService.getMessageCountByStatus();
//
//		Map<String, Object> response = new HashMap<>();
//		response.put("counts", countByStatus);
//		response.put("timestamp", Instant.now());
//
//		return ResponseEntity.ok(response);
//	}
//
//	@GetMapping("/messages")
//	@TracedOperation("getDlqMessages")
//	public ResponseEntity<List<Map<String, Object>>> getPendingMessages(
//			@RequestParam(defaultValue = "20") int limit) {
//		List<Map<String, Object>> messages = dlqRepositoryService.getPendingMessages(limit);
//		return ResponseEntity.ok(messages);
//	}
//}