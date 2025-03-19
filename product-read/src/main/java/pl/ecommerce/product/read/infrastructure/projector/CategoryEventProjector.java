package pl.ecommerce.product.read.infrastructure.projector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import pl.ecommerce.commons.kafka.DomainEventHandler;
import pl.ecommerce.commons.kafka.EventHandler;
import pl.ecommerce.commons.kafka.TopicsProvider;
import pl.ecommerce.product.read.domain.model.CategoryReadModel;
import pl.ecommerce.product.read.infrastructure.repository.CategoryReadRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import static pl.ecommerce.product.read.infrastructure.projector.CategoryEventProjectorHelper.buildCategoryReadModel;
import static pl.ecommerce.product.read.infrastructure.projector.ProductEventProjectorHelper.buildUpdateForEvent;
import static pl.ecommerce.product.read.infrastructure.projector.ProductEventProjectorHelper.extractTraceId;

@Component
@Slf4j
public class CategoryEventProjector extends DomainEventHandler {

	private final CategoryReadRepository categoryRepository;

	public CategoryEventProjector(CategoryReadRepository categoryRepository,
								  ObjectMapper objectMapper, TopicsProvider topicsProvider) {
		super(objectMapper, topicsProvider);
		this.categoryRepository = categoryRepository;
	}

	@EventHandler
	public void on(CategoryCreatedEvent event) {
		String traceId = extractTraceId(event);
		String spanId = event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null;

		log.info("Projecting CategoryCreatedEvent for category: {}, traceId: {}",
				event.getAggregateId(), traceId);

		CategoryReadModel category = buildCategoryReadModel(event, traceId);

		// Dodajemy spanId, który wcześniej był pominięty
		category.setLastSpanId(spanId);

		// Aktualizacja rodzica, jeśli istnieje
		if (event.getParentCategoryId() != null) {
			updateParentCategory(event.getParentCategoryId(), event.getCategoryId(), event.getName(), traceId, spanId);
		}

		categoryRepository.save(category)
				.doOnSuccess(saved -> log.debug("Category read model saved successfully: {}, traceId: {}",
						saved.getId(), traceId))
				.doOnError(error -> log.error("Error saving category read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}

	@EventHandler
	public void on(CategoryUpdatedEvent event) {
		String traceId = extractTraceId(event);
		String spanId = event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null;

		log.info("Projecting CategoryUpdatedEvent for category: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Update update = buildUpdateForEvent(event, traceId);
		// Dodajemy spanId, który wcześniej był pominięty
		if (Objects.nonNull(spanId)) {
			update.set("lastSpanId", spanId);
		}

		categoryRepository.updateCategory(event.getAggregateId(), update, traceId, spanId)
				.doOnSuccess(result -> log.debug("Updated category read model: {}, modified: {}, traceId: {}",
						event.getAggregateId(), result.getModifiedCount(), traceId))
				.doOnError(error -> log.error("Error updating category read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();

		// Sprawdzenie aktualizacji nazwy
		if (event.getChanges().containsKey("name")) {
			String newName = (String) event.getChanges().get("name");

			// Aktualizacja nazwy w podkategoriach
			Query subcategoryQuery = Query.query(Criteria.where("parentCategoryId").is(event.getAggregateId()));
			Update subcategoryUpdate = new Update()
					.set("parentCategoryName", newName)
					.set("lastUpdatedAt", Instant.now())
					.set("lastTraceId", traceId);

			if (Objects.nonNull(spanId)) {
				subcategoryUpdate.set("lastSpanId", spanId);
			}

			categoryRepository.updateMulti(subcategoryQuery, subcategoryUpdate)
					.doOnSuccess(result -> log.debug("Updated subcategories (parent name): {}, modified: {}, traceId: {}",
							event.getAggregateId(), result.getModifiedCount(), traceId))
					.subscribe();

			// Aktualizacja nazwy w kategoriach nadrzędnych (listy nazw podkategorii)
			categoryRepository.findBySubcategoryIdsContaining(event.getAggregateId())
					.flatMap(parent -> {
						// Znalezienie i aktualizacja listy nazw podkategorii
						int index = parent.getSubcategoryNames().indexOf(event.getChanges().get("name"));
						if (index != -1) {
							parent.getSubcategoryNames().set(index, newName);

							// Aktualizacja informacji o śledzeniu
							parent.setLastTraceId(traceId);
							parent.setLastSpanId(spanId);
							parent.setLastOperation("UpdateSubcategoryName");
							parent.setLastUpdatedAt(Instant.now());

							return categoryRepository.save(parent);
						}
						return Mono.just(parent);
					})
					.subscribe();
		}
	}

	@EventHandler
	public void on(SubcategoryAddedEvent event) {
		String traceId = extractTraceId(event);
		String spanId = event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null;

		log.info("Projecting SubcategoryAddedEvent for category: {}, subcategory: {}, traceId: {}",
				event.getAggregateId(), event.getSubcategoryId(), traceId);

		// Pobieranie nazwy podkategorii
		categoryRepository.findById(event.getSubcategoryId())
				.flatMap(subcategory -> {
					// Aktualizacja kategorii nadrzędnej
					return categoryRepository.findById(event.getAggregateId())
							.flatMap(parent -> {
								// Dodanie ID podkategorii do nadrzędnej
								if (parent.getSubcategoryIds() == null) {
									parent.setSubcategoryIds(new HashSet<>());
								}
								parent.getSubcategoryIds().add(event.getSubcategoryId());

								// Dodanie nazwy podkategorii do nadrzędnej
								if (parent.getSubcategoryNames() == null) {
									parent.setSubcategoryNames(new java.util.ArrayList<>());
								}
								parent.getSubcategoryNames().add(subcategory.getName());

								// Aktualizacja informacji o śledzeniu
								parent.setUpdatedAt(event.getTimestamp());
								parent.setLastTraceId(traceId);
								parent.setLastSpanId(spanId);
								parent.setLastOperation("AddSubcategory");
								parent.setLastUpdatedAt(Instant.now());

								return categoryRepository.save(parent);
							});
				})
				.doOnSuccess(saved -> log.debug("Added subcategory relationship: parent={}, child={}, traceId: {}",
						event.getAggregateId(), event.getSubcategoryId(), traceId))
				.doOnError(error -> log.error("Error adding subcategory relationship: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();

		// Aktualizacja podkategorii - ustawienie rodzica
		categoryRepository.findById(event.getSubcategoryId())
				.flatMap(subcategory -> {
					subcategory.setParentCategoryId(event.getAggregateId());

					// Pobranie nazwy rodzica
					return categoryRepository.findById(event.getAggregateId())
							.flatMap(parent -> {
								subcategory.setParentCategoryName(parent.getName());

								// Aktualizacja informacji o śledzeniu
								subcategory.setLastTraceId(traceId);
								subcategory.setLastSpanId(spanId);
								subcategory.setLastOperation("SetParentCategory");
								subcategory.setLastUpdatedAt(Instant.now());

								return categoryRepository.save(subcategory);
							});
				})
				.doOnSuccess(saved -> log.debug("Updated subcategory with parent info: {}, traceId: {}",
						event.getSubcategoryId(), traceId))
				.subscribe();
	}

	@EventHandler
	public void on(CategoryDeletedEvent event) {
		String traceId = extractTraceId(event);
		String spanId = event.getTracingContext() != null ? event.getTracingContext().getSpanId() : null;

		log.info("Projecting CategoryDeletedEvent for category: {}, traceId: {}",
				event.getAggregateId(), traceId);

		Update update = new Update()
				.set("active", false)
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastOperation", "DeleteCategory")
				.set("lastUpdatedAt", Instant.now());

		if (Objects.nonNull(spanId)) {
			update.set("lastSpanId", spanId);
		}

		categoryRepository.updateCategory(event.getAggregateId(), update, traceId, spanId)
				.doOnSuccess(result -> log.debug("Marked category as deleted in read model: {}, traceId: {}",
						event.getAggregateId(), traceId))
				.doOnError(error -> log.error("Error marking category as deleted in read model: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();

		// Usunięcie powiązań z kategorii nadrzędnej, jeśli istnieje
		categoryRepository.findById(event.getAggregateId())
				.flatMap(category -> {
					if (category.getParentCategoryId() != null) {
						return categoryRepository.findById(category.getParentCategoryId())
								.flatMap(parent -> {
									parent.getSubcategoryIds().remove(event.getAggregateId());
									parent.getSubcategoryNames().remove(category.getName());

									// Aktualizacja informacji o śledzeniu
									parent.setLastTraceId(traceId);
									parent.setLastSpanId(spanId);
									parent.setLastOperation("RemoveSubcategory");
									parent.setLastUpdatedAt(Instant.now());

									return categoryRepository.save(parent);
								});
					}
					return Mono.empty();
				})
				.subscribe();
	}

	private void updateParentCategory(UUID parentId, UUID childId, String childName, String traceId, String spanId) {
		categoryRepository.findById(parentId)
				.flatMap(parent -> {
					// Inicjalizacja kolekcji, jeśli potrzebne
					if (parent.getSubcategoryIds() == null) {
						parent.setSubcategoryIds(new HashSet<>());
					}
					if (parent.getSubcategoryNames() == null) {
						parent.setSubcategoryNames(new java.util.ArrayList<>());
					}

					// Dodanie relacji podkategorii
					parent.getSubcategoryIds().add(childId);
					parent.getSubcategoryNames().add(childName);

					// Aktualizacja informacji o śledzeniu
					parent.setLastTraceId(traceId);
					parent.setLastSpanId(spanId);
					parent.setLastOperation("AddSubcategory");
					parent.setLastUpdatedAt(Instant.now());

					return categoryRepository.save(parent);
				})
				.doOnSuccess(saved -> log.debug("Updated parent category with subcategory: parent={}, child={}, traceId: {}",
						parentId, childId, traceId))
				.doOnError(error -> log.error("Error updating parent category: {}, traceId: {}",
						error.getMessage(), traceId, error))
				.subscribe();
	}
}