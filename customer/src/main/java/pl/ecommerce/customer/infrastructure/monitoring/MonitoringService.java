package pl.ecommerce.customer.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
public class MonitoringService {

	private final Counter customerCreatedCounter;
	private final Counter customerUpdatedCounter;
	private final Counter customerDeletedCounter;
	private final Counter failedOperationsCounter;

	private final Timer databaseOperationsTimer;
	private final Timer externalServiceCallsTimer;

	public MonitoringService(MeterRegistry meterRegistry) {

		this.customerCreatedCounter = Counter.builder("customer.operations")
				.tag("type", "created")
				.description("Counter for created customers")
				.register(meterRegistry);

		this.customerUpdatedCounter = Counter.builder("customer.operations")
				.tag("type", "updated")
				.description("Counter for updated customers")
				.register(meterRegistry);

		this.customerDeletedCounter = Counter.builder("customer.operations")
				.tag("type", "deleted")
				.description("Counter for deleted customers")
				.register(meterRegistry);

		this.failedOperationsCounter = Counter.builder("customer.operations")
				.tag("type", "failed")
				.description("Counter for failed operations")
				.register(meterRegistry);

		this.databaseOperationsTimer = Timer.builder("customer.timers")
				.tag("type", "database")
				.description("Timer for database operations")
				.register(meterRegistry);

		this.externalServiceCallsTimer = Timer.builder("customer.timers")
				.tag("type", "external")
				.description("Timer for external service calls")
				.register(meterRegistry);
	}

	public void incrementCustomerCreated() {
		customerCreatedCounter.increment();
		log.debug("Registered customer creation");
	}

	public void incrementCustomerUpdated() {
		customerUpdatedCounter.increment();
		log.debug("Registered customer update");
	}

	public void incrementCustomerDeleted() {
		customerDeletedCounter.increment();
		log.debug("Registered customer deletion");
	}

	public void incrementFailedOperation() {
		failedOperationsCounter.increment();
		log.warn("Registered failed operation");
	}

	public void recordDatabaseOperationTime(long millis) {
		databaseOperationsTimer.record(millis, TimeUnit.MILLISECONDS);
		log.debug("Recorded database operation time: {} ms", millis);
	}

	public void recordExternalServiceCallTime(long millis) {
		externalServiceCallsTimer.record(millis, TimeUnit.MILLISECONDS);
		log.debug("Recorded external service call time: {} ms", millis);
	}

	public <T> Mono<T> measureDatabaseOperation(Supplier<Mono<T>> operation) {
		long startTime = System.currentTimeMillis();

		return operation.get()
				.doFinally(signalType -> {
					long executionTime = System.currentTimeMillis() - startTime;
					recordDatabaseOperationTime(executionTime);
				});
	}

	public <T> Mono<T> measureExternalServiceCall(Supplier<Mono<T>> operation) {
		long startTime = System.currentTimeMillis();

		return operation.get()
				.doFinally(signalType -> {
					long executionTime = System.currentTimeMillis() - startTime;
					recordExternalServiceCallTime(executionTime);
				})
				.doOnError(error -> {
					incrementFailedOperation();
					log.error("Error during external service call: {}", error.getMessage());
				});
	}
}