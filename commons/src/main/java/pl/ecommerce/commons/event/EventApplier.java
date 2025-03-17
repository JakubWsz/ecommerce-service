package pl.ecommerce.commons.event;

@FunctionalInterface
public interface EventApplier {
	void apply(DomainEvent event);
}