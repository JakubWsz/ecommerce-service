package pl.ecommerce.vendor.write.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import pl.ecommerce.vendor.write.domain.VendorAggregate;
import pl.ecommerce.vendor.write.infrastructure.eventstore.EventStore;
import pl.ecommerce.vendor.write.infrastructure.exception.VendorNotFoundException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EventSourcedVendorRepository implements VendorRepository {

	private final EventStore eventStore;
	private final ConcurrentHashMap<UUID, VendorAggregate> cache = new ConcurrentHashMap<>();

	@Override
	public Mono<VendorAggregate> findById(UUID id) {
		if (cache.containsKey(id)) {
			log.debug("Vendor found in cache: {}", id);
			return Mono.just(cache.get(id));
		}

		return eventStore.getEvents(id)
				.collectList()
				.flatMap(events -> {
					if (events.isEmpty()) {
						log.warn("No events found for vendor: {}", id);
						return Mono.error(new VendorNotFoundException("Vendor not found with ID: " + id));
					}

					VendorAggregate vendor = new VendorAggregate(events);
					cache.put(id, vendor);
					log.debug("Vendor loaded from event store: {}", id);
					return Mono.just(vendor);
				});
	}

	@Override
	public Mono<VendorAggregate> save(VendorAggregate vendor) {
		if (vendor.getUncommittedEvents().isEmpty()) {
			log.debug("No uncommitted events to save for vendor: {}", vendor.getId());
			return Mono.just(vendor);
		}

		return eventStore.saveEvents(vendor.getId(), new ArrayList<>(vendor.getUncommittedEvents()))
				.then(Mono.fromCallable(() -> {
					log.debug("Saved {} events for vendor: {}", vendor.getUncommittedEvents().size(), vendor.getId());
					vendor.clearUncommittedEvents();
					cache.put(vendor.getId(), vendor);
					return vendor;
				}));
	}

	@Override
	public Mono<Boolean> existsByEmail(String email) {
		boolean foundInCache = cache.values().stream()
				.anyMatch(vendor -> email.equalsIgnoreCase(vendor.getEmail()));

		if (foundInCache) {
			log.debug("Vendor with email {} found in cache", email);
			return Mono.just(true);
		}

		return eventStore.existsByEmail(email);
	}

}