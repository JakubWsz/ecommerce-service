package pl.ecommerce.customer.write.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update customer preferences")
public record UpdatePreferencesRequest(
		@NotNull @Schema(description = "Marketing consent flag", example = "true")
		boolean marketingConsent,
		@NotNull @Schema(description = "Newsletter subscription flag", example = "false")
		boolean newsletterSubscribed,
		@NotBlank @Schema(description = "Preferred language", example = "en")
		String preferredLanguage,
		@NotBlank @Schema(description = "Preferred currency", example = "USD")
		String preferredCurrency,
		@NotNull @Schema(description = "List of favorite categories", example = "[\"electronics\", \"books\"]")
		List<String> favoriteCategories
) {
}