package pl.ecommerce.eventstore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pl.ecommerce.commons.event.DomainEvent;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventStreamController {

	private final EventStoreService eventStoreService;

	@GetMapping(value = "/stream/{correlationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<DomainEvent> streamEventsByCorrelationId(@PathVariable String correlationId) {
		log.info("Streaming events for correlationId: {}", correlationId);
		return eventStoreService.getEventsByCorrelationId(UUID.fromString(correlationId));
	}

	@GetMapping(value = "/customer/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<DomainEvent> getEventsByCustomerId(@PathVariable UUID customerId) {
		log.info("Fetching events for customerId: {}", customerId);
		return eventStoreService.getEventsByCustomerId(customerId);
	}

	@GetMapping(value = "/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<DomainEvent> getEventsByEmail(@PathVariable String email) {
		log.info("Fetching events for email: {}", email);
		return eventStoreService.getEventsByEmail(email);
	}

	@GetMapping(value = "/date/{date}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<DomainEvent> getEventsByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		log.info("Fetching events for date: {}", date);
		return eventStoreService.getEventsByDate(date.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

	@GetMapping(value = "/range", produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<DomainEvent> getEventsBetweenDates(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		log.info("Fetching events between dates: {} - {}", from, to);
		return eventStoreService.getEventsBetweenDates(
				from.atStartOfDay(ZoneOffset.UTC).toInstant(),
				to.atStartOfDay(ZoneOffset.UTC).toInstant()
		);
	}
}
