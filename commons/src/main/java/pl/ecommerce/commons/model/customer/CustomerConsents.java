package pl.ecommerce.commons.model.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerConsents {
	private boolean gdprConsent;
	private boolean marketingConsent;
	private boolean dataProcessingConsent;
}
