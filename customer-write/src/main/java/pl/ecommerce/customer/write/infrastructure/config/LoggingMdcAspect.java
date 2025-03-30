package pl.ecommerce.customer.write.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import pl.ecommerce.commons.tracing.TracedOperation;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingMdcAspect {

	@Around("@annotation(pl.ecommerce.commons.tracing.TracedOperation)")
	public Object setupMdc(ProceedingJoinPoint joinPoint) throws Throwable {
		TracedOperation tracedOperation = null;
		try {
			tracedOperation = joinPoint.getTarget().getClass().getMethod(
					joinPoint.getSignature().getName(),
					((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes()
			).getAnnotation(TracedOperation.class);
		} catch (NoSuchMethodException e) {
			log.warn("Could not extract traced operation annotation", e);
		}

		String operationName = tracedOperation != null ? tracedOperation.value() : "unknown";

		ServerWebExchange exchange = null;
		for (Object arg : joinPoint.getArgs()) {
			if (arg instanceof ServerWebExchange) {
				exchange = (ServerWebExchange) arg;
				break;
			}
		}

		if (exchange == null) {
			return joinPoint.proceed();
		}

		String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}

		MDC.put("traceId", traceId);
		MDC.put("operation", operationName);

		try {
			Object result = joinPoint.proceed();

			if (result instanceof Mono<?>) {
				String finalTraceId = traceId;

				return ((Mono<?>) result)
						.contextWrite(Context.of("traceId", traceId, "operation", operationName))
						.doOnEach(signal -> {
							if (signal.isOnNext() || signal.isOnError()) {
								MDC.put("traceId", finalTraceId);
								MDC.put("operation", operationName);
							}
						})
						.doFinally(signalType -> MDC.clear());
			}

			return result;
		} finally {
			MDC.clear();
		}
	}
}