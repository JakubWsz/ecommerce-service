package pl.ecommerce.vendor.integration.helper;

import lombok.Getter;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Getter
public class Containers {
	private static Containers instance;

	private final MongoDBContainer mongoContainer;
	private final KafkaContainer kafkaContainer;

	private Containers() {
		mongoContainer = new MongoDBContainer(DockerImageName.parse("mongo:6-focal"))
				.withExposedPorts(27017)
				.waitingFor(Wait.forListeningPort())
				.withStartupTimeout(Duration.ofSeconds(120));

		kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"))
				.withExposedPorts(9092)
				.waitingFor(Wait.forListeningPort())
				.withStartupTimeout(Duration.ofSeconds(120));

		mongoContainer.start();
		kafkaContainer.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			mongoContainer.stop();
			kafkaContainer.stop();
		}));
	}

	public static synchronized Containers getInstance() {
		if (instance == null) {
			instance = new Containers();
		}
		return instance;
	}

}