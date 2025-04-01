//package pl.ecommerce.customer.write.infrastructure.config;
//
//import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.api.common.AttributeKey;
//import io.opentelemetry.api.common.Attributes;
//import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.resources.Resource;
//import io.opentelemetry.sdk.trace.SdkTracerProvider;
//import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class OpenTelemetryConfig {
//
//	@Value("${spring.application.name}")
//	private String serviceName;
//
//	@Value("${otel.exporter.otlp.endpoint:http://otel-collector:4317}")
//	private String endpoint;
//
//	@Bean
//	public OpenTelemetry openTelemetry() {
//		Resource resource = Resource.getDefault()
//				.merge(Resource.create(Attributes.of(
//						AttributeKey.stringKey("service.name"), serviceName,
//						AttributeKey.stringKey("service.version"), "1.0.0")));
//
//		OtlpHttpSpanExporter otlpExporter = OtlpHttpSpanExporter.builder()
//				.setEndpoint(endpoint)
//				.build();
//
//		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
//				.addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
//				.setResource(resource)
//				.build();
//
//		return OpenTelemetrySdk.builder()
//				.setTracerProvider(sdkTracerProvider)
//				.buildAndRegisterGlobal();
//	}
//}