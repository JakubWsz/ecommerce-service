package pl.ecommerce.commons.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "event.listener")
public class TopicsProvider {

	private List<String> topics;

	public String[] getTopics() {
		return topics != null ? topics.toArray(new String[0]) : new String[0];
	}

	public void setTopics(List<String> topics) {
		this.topics = topics;
	}
}
