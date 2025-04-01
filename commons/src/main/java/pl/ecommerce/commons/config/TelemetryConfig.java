//package pl.ecommerce.commons.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//
//import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.api.common.AttributeKey;
//import io.opentelemetry.api.common.Attributes;
//import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.resources.Resource;
//import io.opentelemetry.sdk.trace.SdkTracerProvider;
//import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
//import io.opentelemetry.context.propagation.ContextPropagators;
//import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
//
//@Configuration
//public class TelemetryConfig {
//
//	@Value("${spring.application.name}")
//	private String serviceName;
//
//	@Value("${opentelemetry.otlp.endpoint:http://otel-collector:4317}")
//	private String otlpEndpoint;
//
//	@Bean
//	public OpenTelemetry openTelemetry(Environment env) {
//		String activeProfiles = String.join(",", env.getActiveProfiles());
//		if (activeProfiles.isEmpty()) {
//			activeProfiles = "default";
//		}
//
//		Resource resource = Resource.getDefault()
//				.merge(Resource.create(Attributes.of(
//						AttributeKey.stringKey("service.name"), serviceName,
//						AttributeKey.stringKey("service.profile"), activeProfiles,
//						AttributeKey.stringKey("deployment.environment"),
//						env.getProperty("deployment.environment", "development")
//				)));
//
//		OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
//				.setEndpoint(otlpEndpoint)
//				.build();
//
//		SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
//				.addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
//				.setResource(resource)
//				.build();
//
//		return OpenTelemetrySdk.builder()
//				.setTracerProvider(tracerProvider)
//				.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
//				.buildAndRegisterGlobal();
//	}
//}