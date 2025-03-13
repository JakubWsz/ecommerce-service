package pl.ecommerce.customer.infrastructure.eventstore;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


/**
 * Health indicator for Event Store that checks if the database is accessible
 * and the event_store table is available.
 */
@Component
public class EventStoreHealthIndicator implements HealthIndicator {

	private final JdbcTemplate jdbcTemplate;

	public EventStoreHealthIndicator(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public Health health() {
		try {
			int rowCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM event_store LIMIT 1",
					Integer.class
			);

			int eventCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM event_store",
					Integer.class
			);

			int unprocessedOutboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM event_outbox WHERE processed = false",
					Integer.class
			);

			// Build health details
			return Health.up()
					.withDetail("totalEvents", eventCount)
					.withDetail("unprocessedOutboxMessages", unprocessedOutboxCount)
					.build();

		} catch (DataAccessException e) {
			return Health.down()
					.withDetail("error", e.getMessage())
					.build();
		}
	}
}
