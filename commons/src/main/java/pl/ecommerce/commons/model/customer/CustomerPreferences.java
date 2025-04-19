package pl.ecommerce.commons.model.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPreferences {
	private boolean marketingConsent;
	private boolean newsletterSubscribed;
	private String preferredLanguage;
	private String preferredCurrency;
	private List<String> favoriteCategories;
}