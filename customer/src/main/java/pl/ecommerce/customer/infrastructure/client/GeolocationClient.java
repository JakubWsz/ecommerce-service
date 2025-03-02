package pl.ecommerce.customer.infrastructure.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeolocationClient {

	private final WebClient.Builder webClientBuilder;

	@Value("${services.geolocation-api.base-url}")
	private String geolocationApiBaseUrl;

	@Value("${services.geolocation-api.api-key}")
	private String apiKey;

	public Mono<GeoLocationResponse> getLocationByIp(String ipAddress) {
		log.info("Fetching geolocation for IP: {}", ipAddress);

		return webClientBuilder.baseUrl(geolocationApiBaseUrl)
				.build()
				.get()
				.uri(uriBuilder -> uriBuilder
						.path("/ipgeo")
						.queryParam("apiKey", apiKey)
						.queryParam("ip", ipAddress)
						.build())
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(GeoLocationResponse.class)
				.doOnError(e -> log.error("Error fetching geolocation for IP: {}", ipAddress, e));
	}

	@Setter
	@Getter
	public static class GeoLocationResponse {
		private String country;
		private String city;
		private String state;
		private String postalCode;
		private Map<String, String> currency;

	}
}