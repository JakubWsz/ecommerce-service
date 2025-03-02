package pl.ecommerce.customer.infrastructure.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external.ipgeolocation")
public class GeoLocationProperties {
	private String url;
	private String key;
}