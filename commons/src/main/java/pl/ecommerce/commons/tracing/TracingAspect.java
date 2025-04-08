//package pl.ecommerce.commons.tracing;
//
//import static java.util.Objects.nonNull;
//import static java.util.Objects.isNull;
//
//import io.opentelemetry.api.GlobalOpenTelemetry;
//import io.opentelemetry.api.trace.Span;
//import io.opentelemetry.api.trace.SpanKind;
//import io.opentelemetry.api.trace.Tracer;
//import io.opentelemetry.context.Context;
//import io.opentelemetry.context.Scope;
//import io.opentelemetry.context.propagation.TextMapGetter;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.slf4j.MDC;
//import org.springframework.stereotype.Component;
//import org.aspectj.lang.annotation.*;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.reflect.MethodSignature;
//import reactor.core.publisher.Mono;
//import reactor.core.publisher.Flux;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//
//import java.lang.reflect.Method;
//
//@Aspect
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class TracingAspect {
//
//	private static final TextMapGetter<HttpHeaders> HTTP_HEADERS_GETTER =
//			new TextMapGetter<>() {
//				@Override
//				public Iterable<String> keys(HttpHeaders carrier) {
//					return carrier.keySet();
//				}
//
//				@Override
//				public String get(HttpHeaders carrier, String key) {
//					return isNull(carrier) ? null : carrier.getFirst(key);
//				}
//			};
//
//	private final Tracer tracer;
//
//	@Around("@within(org.springframework.web.bind.annotation.RestController)")
//	public Object traceWebfluxController(ProceedingJoinPoint pjp) throws Throwable {
//		HttpHeaders headers = null;
//		ServerHttpRequest serverRequest = null;
//		for (Object arg : pjp.getArgs()) {
//			if (arg instanceof ServerHttpRequest) {
//				serverRequest = (ServerHttpRequest) arg;
//				headers = serverRequest.getHeaders();
//				break;
//			}
//			if (arg != null && "org.springframework.web.server.ServerWebExchange".equals(arg.getClass().getName())) {
//				try {
//					Method getRequest = arg.getClass().getMethod("getRequest");
//					serverRequest = (ServerHttpRequest) getRequest.invoke(arg);
//					headers = serverRequest.getHeaders();
//				} catch (Exception e) {
//				}
//				break;
//			}
//			if (arg instanceof HttpHeaders) {
//				headers = (HttpHeaders) arg;
//				break;
//			}
//		}
//		if (headers == null) {
//			headers = HttpHeaders.EMPTY;
//		}
//
//		Context parentOtelContext = GlobalOpenTelemetry.getPropagators()
//				.getTextMapPropagator().extract(Context.current(), headers, HTTP_HEADERS_GETTER);
//
//		String spanName;
//		if (nonNull(serverRequest)) {
//			spanName = serverRequest.getMethod().name() + " " + serverRequest.getURI().getPath();
//		} else {
//			spanName = pjp.getSignature().toShortString();
//		}
//
//		Span serverSpan = tracer.spanBuilder(spanName)
//				.setParent(parentOtelContext)
//				.setSpanKind(SpanKind.SERVER)
//				.startSpan();
//		log.debug("Setting MDC traceId: {}", serverSpan.getSpanContext().getTraceId());
//		MDC.put("traceId", serverSpan.getSpanContext().getTraceId());
//		MDC.put("spanId", serverSpan.getSpanContext().getSpanId());
//
//		Object result;
//		try (Scope scope = serverSpan.makeCurrent()) {
//			result = pjp.proceed();
//		}
//
//		if (result instanceof Mono) {
//			@SuppressWarnings("unchecked")
//			Mono<Object> monoResult = (Mono<Object>) result;
//			return monoResult
//					.doOnSubscribe(s -> {
//						MDC.put("traceId", serverSpan.getSpanContext().getTraceId());
//						MDC.put("spanId", serverSpan.getSpanContext().getSpanId());
//					})
//					.doFinally(signalType -> {
//						serverSpan.end();
//						MDC.remove("traceId");
//						MDC.remove("spanId");
//					});
//		} else if (result instanceof Flux) {
//			@SuppressWarnings("unchecked")
//			Flux<Object> fluxResult = (Flux<Object>) result;
//			return fluxResult
//					.doOnSubscribe(s -> {
//						MDC.put("traceId", serverSpan.getSpanContext().getTraceId());
//						MDC.put("spanId", serverSpan.getSpanContext().getSpanId());
//					})
//					.doFinally(signalType -> {
//						serverSpan.end();
//						MDC.remove("traceId");
//						MDC.remove("spanId");
//					});
//		} else {
//			serverSpan.end();
//			MDC.remove("traceId");
//			MDC.remove("spanId");
//			return result;
//		}
//	}
//
//	@Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
//	public Object traceKafkaListener(ProceedingJoinPoint pjp) throws Throwable {
//		Context parentContext = Context.current();
//		String spanName = pjp.getSignature().getName();
//		Object[] args = pjp.getArgs();
//		ConsumerRecord<?, ?> kafkaRecord;
//		for (Object arg : args) {
//			if (arg instanceof ConsumerRecord) {
//				kafkaRecord = (ConsumerRecord<?, ?>) arg;
//				parentContext = KafkaTracingPropagator.extract(Context.current(), kafkaRecord.headers());
//				spanName = "Kafka consume " + kafkaRecord.topic();
//				break;
//			}
//		}
//		Span consumerSpan = tracer.spanBuilder(spanName)
//				.setParent(parentContext)
//				.setSpanKind(SpanKind.CONSUMER)
//				.startSpan();
//		MDC.put("traceId", consumerSpan.getSpanContext().getTraceId());
//		MDC.put("spanId", consumerSpan.getSpanContext().getSpanId());
//		log.debug("Setting MDC traceId: {}", consumerSpan.getSpanContext().getTraceId());
//
//		Object result;
//		try (Scope scope = consumerSpan.makeCurrent()) {
//			result = pjp.proceed();
//		}
//		consumerSpan.end();
//		MDC.remove("traceId");
//		MDC.remove("spanId");
//		return result;
//	}
//
//	@Around("execution(* org.springframework.kafka.core.KafkaTemplate.send(..))")
//	public Object traceKafkaSend(ProceedingJoinPoint pjp) throws Throwable {
//		Object[] args = pjp.getArgs();
//		if (args.length > 0 && args[0] instanceof ProducerRecord) {
//			@SuppressWarnings("unchecked")
//			ProducerRecord<Object, Object> record =
//					(ProducerRecord<Object, Object>) args[0];
//			KafkaTracingPropagator.inject(Context.current(), record.headers());
//			return pjp.proceed();
//		} else {
//			String topic = null;
//			Integer partition = null;
//			Object key = null;
//			Object value = null;
//			MethodSignature methodSig = (MethodSignature) pjp.getSignature();
//			Class<?>[] paramTypes = methodSig.getMethod().getParameterTypes();
//			int paramCount = paramTypes.length;
//			if (paramCount >= 2 && paramTypes[0] == String.class) {
//				topic = (String) args[0];
//				if (paramCount == 2) {
//					value = args[1];
//				} else if (paramCount == 3) {
//					key = args[1];
//					value = args[2];
//				} else if (paramCount >= 4) {
//					partition = (Integer) args[1];
//					key = args[2];
//					value = args[3];
//				}
//			}
//			if (topic != null) {
//				ProducerRecord<Object, Object> record =
//						new ProducerRecord<>(topic, partition, key, value);
//				KafkaTracingPropagator.inject(Context.current(), record.headers());
//				Object kafkaTemplate = pjp.getTarget();
//				try {
//					Method sendMethod = kafkaTemplate.getClass().getMethod("send", ProducerRecord.class);
//					return sendMethod.invoke(kafkaTemplate, record);
//				} catch (NoSuchMethodException e) {
//					return pjp.proceed();
//				}
//			} else {
//				return pjp.proceed();
//			}
//		}
//	}
//}
