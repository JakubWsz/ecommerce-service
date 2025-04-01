//package pl.ecommerce.commons.config;
//
//import ch.qos.logback.classic.LoggerContext;
//import jakarta.annotation.PostConstruct;
//import net.logstash.logback.appender.LogstashTcpSocketAppender;
//import net.logstash.logback.encoder.LogstashEncoder;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//
//@Slf4j
//@Configuration
//public class LoggingConfig {
//
//	@Value("${logging.logstash.host:localhost}")
//	private String logstashHost;
//
//	@Value("${logging.logstash.port:5000}")
//	private int logstashPort;
//
//	@PostConstruct
//	public void setupLogging() {
//		log.info("Logstash init: {}:{}", logstashHost, logstashPort);
//		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//
//		LogstashTcpSocketAppender logstashAppender = new LogstashTcpSocketAppender();
//		logstashAppender.setName("logstash");
//		logstashAppender.setContext(loggerContext);
//		logstashAppender.addDestination(logstashHost + ":" + logstashPort);
//
//		LogstashEncoder encoder = new LogstashEncoder();
//		encoder.setIncludeMdc(true);
//		encoder.setContext(loggerContext);
//		encoder.start();
//
//		logstashAppender.setEncoder(encoder);
//		logstashAppender.start();
//
//		ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
//		rootLogger.addAppender(logstashAppender);
//	}
//}