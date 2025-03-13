package pl.ecommerce.customer.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.DomainEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.customer.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.infrastructure.eventstore.EventStore;
import pl.ecommerce.customer.infrastructure.exception.ConcurrencyException;
import pl.ecommerce.customer.infrastructure.outbox.OutboxRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacja repozytorium dla agregatów klienta korzystająca z Event Sourcing
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class EventSourcedCustomerRepository implements CustomerRepository {

	private final EventStore eventStore;
	private final OutboxRepository outboxRepository;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Zapisuje agregat Customer do Event Store
	 * @param customer Agregat klienta
	 * @return Mono z zapisanym agregatem
	 */
	@Override
	@Transactional
	public Mono<CustomerAggregate> save(CustomerAggregate customer) {
		List<DomainEvent> uncommittedEvents = customer.getUncommittedEvents();

		if (uncommittedEvents.isEmpty()) {
			return Mono.just(customer);
		}

		try {
			// Zapisz zdarzenia w Event Store - oblicz oczekiwaną wersję przed dodaniem nowych zdarzeń
			int expectedVersion = customer.getVersion() - uncommittedEvents.size();
			eventStore.saveEvents(customer.getId(), uncommittedEvents, expectedVersion);

			// Zapisz zdarzenia w Outbox (dla asynchronicznej publikacji)
			uncommittedEvents.forEach(outboxRepository::save);

			// Wyczyść niezapisane zdarzenia
			customer.clearUncommittedEvents();

			log.debug("Saved customer: {} with {} events", customer.getId(), uncommittedEvents.size());
			return Mono.just(customer);

		} catch (ConcurrencyException e) {
			log.error("Concurrency conflict when saving customer {}: {}", customer.getId(), e.getMessage());
			return Mono.error(e);
		} catch (Exception e) {
			log.error("Error saving customer {}: {}", customer.getId(), e.getMessage(), e);
			return Mono.error(e);
		}
	}

	/**
	 * Znajduje agregat Customer na podstawie ID
	 * @param customerId ID klienta
	 * @return Optional zawierający znaleziony agregat lub pusty
	 */
	@Override
	@Transactional(readOnly = true)
	public Mono<CustomerAggregate> findById(UUID customerId) {
		try {
			List<DomainEvent> events = eventStore.getEventsForAggregate(customerId);

			if (events.isEmpty()) {
				log.debug("No events found for customer: {}", customerId);
				return Mono.empty();
			}

			// Odtwórz agregat z zdarzeń
			CustomerAggregate customer = new CustomerAggregate(events);
			log.debug("Reconstituted customer {} from {} events", customerId, events.size());
			return Mono.just(customer);

		} catch (Exception e) {
			log.error("Error finding customer by id {}: {}", customerId, e.getMessage(), e);
			return Mono.error(e);
		}
	}

	/**
	 * Sprawdza, czy istnieje klient o podanym adresie email
	 * @param email Adres email do sprawdzenia
	 * @return Mono z wartością boolean
	 */
	@Override
	@Transactional(readOnly = true)
	public Mono<Boolean> existsByEmail(String email) {
		try {
			String sql = "SELECT COUNT(*) FROM customer_email_view WHERE email = ?";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
			return Mono.just(count != null && count > 0);
		} catch (DataAccessException e) {
			log.error("Error checking if customer exists by email {}: {}", email, e.getMessage(), e);
			return Mono.error(e);
		}
	}

	/**
	 * Znajduje agregat Customer na podstawie adresu email
	 * @param email Adres email klienta
	 * @return Mono zawierający znaleziony agregat lub pusty
	 */
	@Override
	@Transactional(readOnly = true)
	public Mono<CustomerAggregate> findByEmail(String email) {
		try {
			String sql = "SELECT customer_id FROM customer_email_view WHERE email = ? LIMIT 1";
			List<UUID> customerIds = jdbcTemplate.query(
					sql,
					(rs, rowNum) -> UUID.fromString(rs.getString("customer_id")),
					email
			);

			if (customerIds.isEmpty()) {
				return Mono.empty();
			}

			return findById(customerIds.get(0));

		} catch (DataAccessException e) {
			log.error("Error finding customer by email {}: {}", email, e.getMessage(), e);
			return Mono.error(e);
		}
	}

	/**
	 * Usuwaia fizycznie dane klienta (wykorzystywane w GDPR)
	 * @param customerId ID klienta
	 * @return Mono po zakończeniu operacji
	 */
	@Override
	@Transactional
	public Mono<Void> hardDelete(UUID customerId) {
		try {
			eventStore.markEventsAsDeleted(customerId);
			log.info("Hard deleted customer: {}", customerId);
			return Mono.empty();
		} catch (Exception e) {
			log.error("Error hard deleting customer {}: {}", customerId, e.getMessage(), e);
			return Mono.error(e);
		}
	}

	@Override
	public Flux<CustomerAggregate> findAll() {
		return null;
	}
}
