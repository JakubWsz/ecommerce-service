package pl.ecommerce.customer.domain.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "customers")
public class Customer {
	@Id
	private UUID id;
	@Field("active")
	private boolean active;
	@Field("registrationIp")
	private String registrationIp;
	@Field("createdAt")
	private LocalDateTime createdAt;
	@Field("updatedAt")
	private LocalDateTime updatedAt;
	@Field("lastLoginAt")
	private LocalDateTime lastLoginAt;
	@Field("gdprConsent")
	private boolean gdprConsent;
	@Field("consentTimestamp")
	private LocalDateTime consentTimestamp;
	@Field("marketingConsent")
	private boolean marketingConsent;
	@Field("dataProcessingConsent")
	private boolean dataProcessingConsent;
	@Field("dataRetentionPeriodDays")
	private Integer dataRetentionPeriodDays;
	@Field("personalData")
	private PersonalData personalData;
	@Field("addresses")
	private List<Address> addresses;
	@Field("geoLocationData")
	private GeoLocationData geoLocationData;
	@Field("currencies")
	private List<MonetaryAmount> currencies;

	public Customer(boolean active, String registrationIp, LocalDateTime createdAt, LocalDateTime updatedAt,
					LocalDateTime lastLoginAt, boolean gdprConsent, LocalDateTime consentTimestamp,
					boolean marketingConsent, boolean dataProcessingConsent, Integer dataRetentionPeriodDays,
					PersonalData personalData, List<Address> addresses, GeoLocationData geoLocationData,
					List<MonetaryAmount> currencies) {
		this.id = UUID.randomUUID();
		this.active = active;
		this.registrationIp = registrationIp;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.lastLoginAt = lastLoginAt;
		this.gdprConsent = gdprConsent;
		this.consentTimestamp = consentTimestamp;
		this.marketingConsent = marketingConsent;
		this.dataProcessingConsent = dataProcessingConsent;
		this.dataRetentionPeriodDays = dataRetentionPeriodDays;
		this.personalData = personalData;
		this.addresses = addresses;
		this.geoLocationData = geoLocationData;
		this.currencies = currencies;
	}

	public Customer(Customer other) {
		this.id = other.id;
		this.active = other.active;
		this.registrationIp = other.registrationIp;
		this.createdAt = other.createdAt;
		this.updatedAt = other.updatedAt;
		this.lastLoginAt = other.lastLoginAt;
		this.gdprConsent = other.gdprConsent;
		this.consentTimestamp = other.consentTimestamp;
		this.marketingConsent = other.marketingConsent;
		this.dataProcessingConsent = other.dataProcessingConsent;
		this.dataRetentionPeriodDays = other.dataRetentionPeriodDays;
		this.personalData = other.personalData != null ? new PersonalData(other.personalData) : null;
		this.addresses = other.addresses != null
				? other.addresses.stream().map(Address::new).collect(Collectors.toList())
				: null;
		this.geoLocationData = other.geoLocationData != null ? new GeoLocationData(other.geoLocationData) : null;
		this.currencies = other.currencies;
	}
}