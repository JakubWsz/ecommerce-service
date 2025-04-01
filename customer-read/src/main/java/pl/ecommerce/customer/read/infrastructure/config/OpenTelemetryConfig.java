package pl.ecommerce.customer.read.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenTelemetryConfig {

	@Value("${opentelemetry.otlp.endpoint:http://localhost:4317}")
	private String otlpEndpoint;

	@Bean
	public OpenTelemetry openTelemetry() {
		Resource resource = Resource.create(Attributes.of(
				AttributeKey.stringKey("service.name"), "customer-read",
				AttributeKey.stringKey("service.version"), "1.0.0"
		));

		OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
				.setEndpoint(otlpEndpoint)
				.setTimeout(Duration.ofSeconds(30))
				.build();

		SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
				.addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
				.setResource(resource)
				.build();

		return OpenTelemetrySdk.builder()
				.setTracerProvider(tracerProvider)
				.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
				.buildAndRegisterGlobal();
	}
}