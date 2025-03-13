package pl.ecommerce.customer.api.dto;

import java.util.List;

public record PreferencesRequest(boolean marketingConsent,
								 boolean newsletterSubscribed,
								 String preferredLanguage,
								 String preferredCurrency,
								 List<String> favoriteCategories) {
}
