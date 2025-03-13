package pl.ecommerce.customer.infrastructure.outbox;

import pl.ecommerce.commons.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Interfejs repozytorium dla tabeli outbox
 */
public interface OutboxRepository {

	/**
	 * Zapisuje zdarzenie w tabeli outbox
	 * @param event Zdarzenie domenowe do zapisania
	 */
	void save(DomainEvent event);

	/**
	 * Pobiera nieprzetworzony zdarzenia
	 * @param limit Maksymalna liczba wiadomości do pobrania
	 * @return Lista nieprzetworzonych wiadomości
	 */
	List<OutboxMessage> findUnprocessedMessages(int limit);

	/**
	 * Oznacza wiadomość jako przetworzoną
	 * @param messageId ID wiadomości
	 */
	void markAsProcessed(UUID messageId);

	/**
	 * Inkrementuje licznik prób przetwarzania i zapisuje błąd
	 * @param messageId ID wiadomości
	 * @param errorMessage Komunikat błędu
	 */
	void incrementProcessingAttempts(UUID messageId, String errorMessage);

	/**
	 * Usuwa przetworzone wiadomości starsze niż podany okres
	 * @param timestamp Znacznik czasu graniczny
	 * @return Liczba usuniętych wiadomości
	 */
	int deleteProcessedMessagesBefore(Instant timestamp);
}