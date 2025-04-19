package pl.ecommerce.customer.write.infrastructure;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class TestContainersHelper {

	static {
		System.setProperty("testcontainers.ryuk.disabled", "true");
		System.setProperty("testcontainers.checks.disable", "true");
		System.setProperty("testcontainers.reuse.enable", "true");
	}

	@Container
	private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
			.withDatabaseName("diagnostics")
			.withUsername("test")
			.withPassword("test");

	@Test
	void testDockerEnvironment() {
		System.out.println("=== Docker Environment Diagnostics ===");

		System.out.println("Docker client initialized: " + (DockerClientFactory.instance().client() != null));

		System.out.println("=== Environment Variables ===");
		Map<String, String> env = System.getenv();
		env.forEach((key, value) -> {
			if (key.startsWith("TESTCONTAINERS") || key.startsWith("DOCKER")) {
				System.out.println(key + "=" + value);
			}
		});

		System.out.println("=== System Properties ===");
		System.getProperties().forEach((key, value) -> {
			if (Objects.toString(key).contains("testcontainers") ||
					Objects.toString(key).contains("docker")) {
				System.out.println(key + "=" + value);
			}
		});

		System.out.println("=== Container Test ===");
		System.out.println("PostgreSQL container running: " + postgres.isRunning());
		System.out.println("PostgreSQL container ID: " + postgres.getContainerId());
		System.out.println("PostgreSQL JDBC URL: " + postgres.getJdbcUrl());

		assertThat(postgres.isRunning()).isTrue();
	}
}