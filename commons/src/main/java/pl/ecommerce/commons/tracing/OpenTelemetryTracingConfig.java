package pl.ecommerce.commons.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryTracingConfig {

	@Value("${spring.application.name:default-service}")
	private String serviceName;

	@Bean
	public OpenTelemetry openTelemetry() {
		SpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
				.setEndpoint("http://localhost:4317")
				.build();

		Resource resource = Resource.create(
				Attributes.of(AttributeKey.stringKey("service.name"), serviceName)
		);

		SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
				.setResource(resource)
				.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
				.build();

		return OpenTelemetrySdk.builder()
				.setTracerProvider(tracerProvider)
				.buildAndRegisterGlobal();
	}

	@Bean
	public Tracer tracer(OpenTelemetry openTelemetry) {
		OtelCurrentTraceContext currentTraceContext = new OtelCurrentTraceContext();

		OtelTracer.EventPublisher noopPublisher = event -> {
			// do nothing
		};

		return new OtelTracer(
				openTelemetry.getTracer(serviceName),
				currentTraceContext,
				noopPublisher
		);
	}

	@Bean
	public ObservationRegistry observationRegistry(Tracer tracer) {
		ObservationRegistry registry = ObservationRegistry.create();
		registry.observationConfig().observationHandler(
				new io.micrometer.tracing.handler.DefaultTracingObservationHandler(tracer)
		);
		return registry;
	}
}
