package pl.ecommerce.customer.aplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPreferencesDto {
	private boolean marketingConsent;
	private boolean newsletterSubscribed;
	private String preferredLanguage;
	private String preferredCurrency;
	private List<String> favoriteCategories;
}

