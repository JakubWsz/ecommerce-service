package pl.ecommerce.commons.customer.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerConsents {
	private boolean gdprConsent;
	private boolean marketingConsent;
	private boolean dataProcessingConsent;
}
