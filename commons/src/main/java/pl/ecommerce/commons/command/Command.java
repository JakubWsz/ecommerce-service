package pl.ecommerce.commons.command;

import pl.ecommerce.commons.tracing.TracingContext;

public interface Command {
	TracingContext tracingContext();
}
