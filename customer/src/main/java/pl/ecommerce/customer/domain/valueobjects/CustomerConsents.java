package pl.ecommerce.customer.domain.valueobjects;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerConsents {
	boolean gdprConsent;
	boolean marketingConsent;
	boolean dataProcessingConsent;
}
