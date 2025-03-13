package pl.ecommerce.customer.domain.valueobjects;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomerPreferences {
	boolean marketingConsent;
	boolean newsletterSubscribed;
	String preferredLanguage;
	String preferredCurrency;
	List<String> favoriteCategories;
}
