package pl.ecommerce.commons.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CustomerPreferences (
		 boolean marketingConsent,
		 boolean newsletterSubscribed,
		 String preferredLanguage,
		 String preferredCurrency,
		 List<String> favoriteCategories
){
}
