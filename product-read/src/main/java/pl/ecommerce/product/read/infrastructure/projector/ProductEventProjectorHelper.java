package pl.ecommerce.product.read.infrastructure.projector;

import org.springframework.data.mongodb.core.query.Update;
import pl.ecommerce.commons.event.product.*;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import pl.ecommerce.product.read.domain.model.ProductReadModel.PriceInfo;
import pl.ecommerce.product.read.domain.model.ProductReadModel.StockInfo;
import pl.ecommerce.product.read.domain.model.ProductReadModel.ProductAttribute;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ProductEventProjectorHelper {

	public static ProductReadModel buildProductReadModel(ProductCreatedEvent event, String traceId) {
		TracingContext tracingContext = event.getTracingContext();

		PriceInfo price = new PriceInfo(
				event.getPrice(),
				event.getDiscountedPrice(),
				"USD" // Default currency
		);

		StockInfo stock = new StockInfo(
				event.getInitialStock(),
				0, // No reservations initially
				"DEFAULT" // Default warehouse
		);

		return ProductReadModel.builder()
				.id(event.getProductId())
				.name(event.getName())
				.description(event.getDescription())
				.sku(event.getSku())
				.categoryIds(event.getCategories() != null ? new HashSet<>(event.getCategories()) : new HashSet<>())
				.vendorId(event.getVendorId())
				.price(price)
				.attributes(event.getAttributes() != null ?
						event.getAttributes().stream()
								.map(attr -> new ProductAttribute(attr.getName(), attr.getValue(), attr.getUnit()))
								.collect(Collectors.toList()) :
						new ArrayList<>())
				.stock(stock)
				.status(event.getStatus())
				.images(event.getImages() != null ? new ArrayList<>(event.getImages()) : new ArrayList<>())
				.brandName(event.getBrandName())
				.metadata(new HashMap<>())
				.createdAt(event.getTimestamp())
				.updatedAt(event.getTimestamp())
				.lastTraceId(traceId)
				.lastSpanId(tracingContext != null ? tracingContext.getSpanId() : null)
				.lastOperation("CreateProduct")
				.lastUpdatedAt(Instant.now())
				.build();
	}

	public static Update buildUpdateForEvent(ProductUpdatedEvent event, String traceId) {
		TracingContext tracingContext = event.getTracingContext();
		Update update = new Update()
				.set("updatedAt", event.getTimestamp())
				.set("lastTraceId", traceId)
				.set("lastSpanId", tracingContext != null ? tracingContext.getSpanId() : null)
				.set("lastOperation", "UpdateProduct")
				.set("lastUpdatedAt", Instant.now());

		event.getChanges().forEach((key, value) -> {
			switch (key) {
				case "name":
					update.set("name", value);
					break;
				case "description":
					update.set("description", value);
					break;
				case "categories":
					update.set("categoryIds", value);
					break;
				case "attributes":
					update.set("attributes", ((java.util.Collection<?>) value).stream()
							.filter(a -> a instanceof pl.ecommerce.product.write.domain.valueobjects.ProductAttribute)
							.map(a -> {
								pl.ecommerce.product.write.domain.valueobjects.ProductAttribute attr =
										(pl.ecommerce.product.write.domain.valueobjects.ProductAttribute) a;
								return new ProductAttribute(attr.getName(), attr.getValue(), attr.getUnit());
							})
							.collect(Collectors.toList()));
					break;
				case "images":
					update.set("images", value);
					break;
				case "brandName":
					update.set("brandName", value);
					break;
				case "featured":
					update.set("featured", value);
					break;
			}
		});

		return update;
	}

	public static String extractTraceId(Object event) {
		if (event instanceof ProductCreatedEvent) {
			return ((ProductCreatedEvent) event).getTracingContext() != null ?
					((ProductCreatedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductUpdatedEvent) {
			return ((ProductUpdatedEvent) event).getTracingContext() != null ?
					((ProductUpdatedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductPriceUpdatedEvent) {
			return ((ProductPriceUpdatedEvent) event).getTracingContext() != null ?
					((ProductPriceUpdatedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductStockUpdatedEvent) {
			return ((ProductStockUpdatedEvent) event).getTracingContext() != null ?
					((ProductStockUpdatedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductReservedEvent) {
			return ((ProductReservedEvent) event).getTracingContext() != null ?
					((ProductReservedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductReservationConfirmedEvent) {
			return ((ProductReservationConfirmedEvent) event).getTracingContext() != null ?
					((ProductReservationConfirmedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductReservationReleasedEvent) {
			return ((ProductReservationReleasedEvent) event).getTracingContext() != null ?
					((ProductReservationReleasedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductVariantAddedEvent) {
			return ((ProductVariantAddedEvent) event).getTracingContext() != null ?
					((ProductVariantAddedEvent) event).getTracingContext().getTraceId() : "unknown";
		} else if (event instanceof ProductDeletedEvent) {
			return ((ProductDeletedEvent) event).getTracingContext() != null ?
					((ProductDeletedEvent) event).getTracingContext().getTraceId() : "unknown";
		}
		return "unknown";
	}
}