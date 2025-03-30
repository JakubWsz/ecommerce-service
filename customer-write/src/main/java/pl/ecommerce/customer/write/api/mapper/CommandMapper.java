package pl.ecommerce.customer.write.api.mapper;

import java.util.UUID;

import pl.ecommerce.commons.model.customer.AddressType;
import pl.ecommerce.commons.model.customer.CustomerPreferences;
import pl.ecommerce.customer.write.api.dto.CustomerRegistrationRequest;
import pl.ecommerce.customer.write.api.dto.CustomerUpdateRequest;
import pl.ecommerce.customer.write.api.dto.AddShippingAddressRequest;
import pl.ecommerce.customer.write.api.dto.UpdateShippingAddressRequest;
import pl.ecommerce.customer.write.api.dto.UpdatePreferencesRequest;
import pl.ecommerce.customer.write.domain.commands.RegisterCustomerCommand;
import pl.ecommerce.customer.write.domain.commands.UpdateCustomerCommand;
import pl.ecommerce.customer.write.domain.commands.AddShippingAddressCommand;
import pl.ecommerce.customer.write.domain.commands.UpdateShippingAddressCommand;
import pl.ecommerce.customer.write.domain.commands.RemoveShippingAddressCommand;
import pl.ecommerce.customer.write.domain.commands.UpdateCustomerPreferencesCommand;
import pl.ecommerce.customer.write.domain.commands.DeactivateCustomerCommand;

public interface CommandMapper {

	static RegisterCustomerCommand map(CustomerRegistrationRequest request) {
		return RegisterCustomerCommand.builder()
				.customerId(UUID.randomUUID())
				.email(request.email())
				.firstName(request.firstName())
				.lastName(request.lastName())
				.phoneNumber(request.phoneNumber())
				.password(request.password())
				.consents(request.consents())
				.build();
	}

	static UpdateCustomerCommand map(UUID id, CustomerUpdateRequest request) {
		return UpdateCustomerCommand.builder()
				.customerId(id)
				.firstName(request.firstName())
				.lastName(request.lastName())
				.phoneNumber(request.phoneNumber())
				.build();
	}

	static AddShippingAddressCommand map(UUID id, AddShippingAddressRequest request) {
		return AddShippingAddressCommand.builder()
				.customerId(id)
				.addressType(AddressType.valueOf(request.addressType()))
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.street(request.street())
				.city(request.city())
				.postalCode(request.postalCode())
				.country(request.country())
				.voivodeship(request.voivodeship())
				.isDefault(request.isDefault())
				.build();
	}

	static UpdateShippingAddressCommand map(UUID id, UUID addressId, UpdateShippingAddressRequest request) {
		return UpdateShippingAddressCommand.builder()
				.customerId(id)
				.addressId(addressId)
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.street(request.street())
				.city(request.city())
				.postalCode(request.postalCode())
				.country(request.country())
				.voivodeship(request.voivodeship())
				.isDefault(request.isDefault())
				.build();
	}

	static RemoveShippingAddressCommand map(UUID id, UUID addressId) {
		return RemoveShippingAddressCommand.builder()
				.customerId(id)
				.addressId(addressId)
				.build();
	}

	static UpdateCustomerPreferencesCommand map(UUID id, UpdatePreferencesRequest request) {
		return UpdateCustomerPreferencesCommand.builder()
				.customerId(id)
				.preferences(map(request))
				.build();
	}

	static DeactivateCustomerCommand map(UUID id, String reason) {
		return DeactivateCustomerCommand.builder()
				.customerId(id)
				.reason(reason)
				.build();
	}

	static CustomerPreferences map(UpdatePreferencesRequest request){
		return CustomerPreferences.builder()
				.marketingConsent(request.marketingConsent())
				.newsletterSubscribed(request.newsletterSubscribed())
				.preferredCurrency(request.preferredCurrency())
				.preferredLanguage(request.preferredLanguage())
				.favoriteCategories(request.favoriteCategories())
				.build();
	}
}
