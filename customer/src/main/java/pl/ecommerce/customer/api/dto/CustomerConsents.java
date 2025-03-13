package pl.ecommerce.customer.api.dto;

import lombok.Builder;


@Builder
public record CustomerConsents(boolean gdprConsent,
							   boolean marketingConsent,
							   boolean dataProcessingConsent) {
}
