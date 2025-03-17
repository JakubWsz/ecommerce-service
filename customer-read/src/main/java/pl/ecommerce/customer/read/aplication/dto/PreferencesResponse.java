package pl.ecommerce.customer.read.aplication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@Schema(description = "Customer preferences response")
public class PreferencesResponse {

	@Schema(description = "Flag indicating if marketing consent is given", example = "true")
	private boolean marketingConsent;

	@Schema(description = "Flag indicating if subscribed to newsletter", example = "false")
	private boolean newsletterSubscribed;

	@Schema(description = "Preferred language", example = "en")
	private String preferredLanguage;

	@Schema(description = "Preferred currency", example = "USD")
	private String preferredCurrency;

	@Schema(description = "List of favorite categories", example = "[\"electronics\", \"books\"]")
	private List<String> favoriteCategories;
}
