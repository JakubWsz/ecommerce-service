//package pl.ecommerce.commons.config;
//
//import org.slf4j.MDC;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.stereotype.Component;

//@Component
//public class LoggingMdcInitializer implements ApplicationRunner {
//
//	@Value("${spring.application.name:unknown}")
//	private String appName;
//
//	@Value("${spring.profiles.active:default}")
//	private String profile;
//
//	@Override
//	public void run(ApplicationArguments args) {
//		MDC.put("springAppName", appName);
//		MDC.put("profile", profile);
//	}
//}
