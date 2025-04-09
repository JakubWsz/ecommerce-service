package pl.ecommerce.commons.tracing;

import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.http.HttpHeaders;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.time.Duration;

import static java.util.Objects.nonNull;

@Configuration
public class OpenTelemetryConfig {

	@Value("${spring.application.name:default-service}")
	private String serviceName;

	private static final TextMapSetter<HttpHeaders> HTTP_HEADERS_SETTER =
			(carrier, key, value) -> {
				if (nonNull(carrier) && nonNull(key) && nonNull(value)) {
					carrier.set(key, value);
				}
			};

	@Bean
	public OpenTelemetry openTelemetry() {
		SpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
				.setTimeout(Duration.ofSeconds(5))
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
				.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
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
	public WebClientCustomizer openTelemetryWebClientCustomizer() {
		return builder -> builder.filter(otelFilterFunction());
	}

	private ExchangeFilterFunction otelFilterFunction() {
		final TextMapPropagator textMapPropagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

		return (request, next) -> {
			ClientRequest modifiedRequest = ClientRequest.from(request)
					.headers(httpHeaders -> {
						Context currentContext = Context.current();

						textMapPropagator.inject(currentContext, httpHeaders, HTTP_HEADERS_SETTER);
					})
					.build();
			return next.exchange(modifiedRequest);
		};
	}
}
