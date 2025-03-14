package pl.ecommerce.commons.customer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerPreferences {
	private boolean marketingConsent;
	private boolean newsletterSubscribed;
	private String preferredLanguage;
	private String preferredCurrency;
	private List<String> favoriteCategories;
}