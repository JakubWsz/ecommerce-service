package pl.ecommerce.customer.read.infrastructure.config;

import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

	@Value("${logging.elasticsearch.host:localhost}")
	private String elasticsearchHost;

	@Value("${logging.elasticsearch.port:9200}")
	private int elasticsearchPort;

	@PostConstruct
	public void setupLogging() {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		LogstashTcpSocketAppender logstashAppender = new LogstashTcpSocketAppender();
		logstashAppender.setName("logstash");
		logstashAppender.setContext(loggerContext);
		logstashAppender.addDestination(elasticsearchHost + ":" + elasticsearchPort);

		LogstashEncoder encoder = new LogstashEncoder();
		encoder.setContext(loggerContext);
		encoder.start();

		logstashAppender.setEncoder(encoder);
		logstashAppender.start();

		ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(logstashAppender);
	}
}