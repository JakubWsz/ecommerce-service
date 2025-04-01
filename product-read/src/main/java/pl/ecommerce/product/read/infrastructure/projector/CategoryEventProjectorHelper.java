package pl.ecommerce.product.read.infrastructure.projector;

import org.springframework.data.mongodb.core.query.Update;
import pl.ecommerce.commons.event.product.CategoryCreatedEvent;
import pl.ecommerce.commons.event.product.CategoryUpdatedEvent;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.domain.model.CategoryReadModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CategoryEventProjectorHelper {

	public static CategoryReadModel buildCategoryReadModel(CategoryCreatedEvent event, String traceId) {
		TracingContext tracingContext = event.getTracingContext();

		return CategoryReadModel.builder()
				.id(event.getCategoryId())
				.name(event.getName())
				.description(event.getDescription())
				.slug(event.getSlug())
				.parentCategoryId(event.getParentCategoryId())
				.subcategoryIds(new HashSet<>())
				.subcategoryNames(new ArrayList<>())
				.attributes(event.getAttributes() != null ?
						new HashMap<>(event.getAttributes()) : new HashMap<>())
				.active(event.isActive())
				.iconUrl(event.getIconUrl())
				.imageUrl(event.getImageUrl())
				.displayOrder(event.getDisplayOrder())
				.productCount(0)
				.createdAt(event.getTimestamp())
				.updatedAt(event.getTimestamp())
				.lastTraceId(traceId)
				.lastSpanId(tracingContext != null ? tracingContext.getSpanId() : null)
				.lastOperation("CreateCategory")
				.lastUpdatedAt(Instant.now())
				.build();
	}

	public static Update buildUpdateForEvent(CategoryUpdatedEvent event, String traceId) {
		TracingContext tracingContext = event.getTracingContext();
		Update update = new Update()
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", tracingContext != null ? tracingContext.getSpanId() : null)
				.set("lastOperation", "UpdateCategory")
				.set("lastUpdatedAt", Instant.now());

		event.getChanges().forEach((key, value) -> {
			switch (key) {
				case "name":
					update.set("name", value);
					break;
				case "description":
					update.set("description", value);
					break;
				case "slug":
					update.set("slug", value);
					break;
				case "attributes":
					update.set("attributes", value);
					break;
				case "active":
					update.set("active", value);
					break;
				case "iconUrl":
					update.set("iconUrl", value);
					break;
				case "imageUrl":
					update.set("imageUrl", value);
					break;
				case "displayOrder":
					update.set("displayOrder", value);
					break;
			}
		});

		return update;
	}

	public static String extractTraceId(Object event) {
		if (event instanceof CategoryCreatedEvent) {
			return ((CategoryCreatedEvent) event).getTracingContext() != null ?
					((CategoryCreatedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof CategoryUpdatedEvent) {
			return ((CategoryUpdatedEvent) event).getTracingContext() != null ?
					((CategoryUpdatedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof pl.ecommerce.commons.event.product.SubcategoryAddedEvent) {
			return ((pl.ecommerce.commons.event.product.SubcategoryAddedEvent) event).getTracingContext() != null ?
					((pl.ecommerce.commons.event.product.SubcategoryAddedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof pl.ecommerce.commons.event.product.CategoryDeletedEvent) {
			return ((pl.ecommerce.commons.event.product.CategoryDeletedEvent) event).getTracingContext() != null ?
					((pl.ecommerce.commons.event.product.CategoryDeletedEvent) event).getTracingContext().getTraceId() : "unknown";
		}
		return "unknown";
	}
}