package pl.ecommerce.customer.write.api.mapper;

import java.util.UUID;

import pl.ecommerce.commons.customer.model.AddressType;
import pl.ecommerce.commons.customer.model.CustomerPreferences;
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
import pl.ecommerce.commons.tracing.TracingContext;

public interface CommandMapper {

	static RegisterCustomerCommand map(CustomerRegistrationRequest request, TracingContext tracingContext) {
		return RegisterCustomerCommand.builder()
				.customerId(UUID.randomUUID())
				.email(request.email())
				.firstName(request.firstName())
				.lastName(request.lastName())
				.phoneNumber(request.phoneNumber())
				.password(request.password())
				.consents(request.consents())
				.tracingContext(tracingContext)
				.build();
	}

	static UpdateCustomerCommand map(UUID id, CustomerUpdateRequest request, TracingContext tracingContext) {
		return UpdateCustomerCommand.builder()
				.customerId(id)
				.firstName(request.firstName())
				.lastName(request.lastName())
				.phoneNumber(request.phoneNumber())
				.tracingContext(tracingContext)
				.build();
	}

	static AddShippingAddressCommand map(UUID id, AddShippingAddressRequest request,
										 TracingContext tracingContext) {
		return AddShippingAddressCommand.builder()
				.customerId(id)
				.addressType(AddressType.valueOf(request.addressType()))
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.street(request.street())
				.city(request.city())
				.postalCode(request.postalCode())
				.country(request.country())
				.state(request.state())
				.isDefault(request.isDefault())
				.tracingContext(tracingContext)
				.build();
	}

	static UpdateShippingAddressCommand map(UUID id, UUID addressId, UpdateShippingAddressRequest request,
											TracingContext tracingContext) {
		return UpdateShippingAddressCommand.builder()
				.customerId(id)
				.addressId(addressId)
				.buildingNumber(request.buildingNumber())
				.apartmentNumber(request.apartmentNumber())
				.street(request.street())
				.city(request.city())
				.postalCode(request.postalCode())
				.country(request.country())
				.state(request.state())
				.isDefault(request.isDefault())
				.tracingContext(tracingContext)
				.build();
	}

	static RemoveShippingAddressCommand map(UUID id, UUID addressId, TracingContext tracingContext) {
		return RemoveShippingAddressCommand.builder()
				.customerId(id)
				.addressId(addressId)
				.tracingContext(tracingContext)
				.build();
	}

	static UpdateCustomerPreferencesCommand map(UUID id, UpdatePreferencesRequest request, TracingContext tracingContext) {
		return UpdateCustomerPreferencesCommand.builder()
				.customerId(id)
				.preferences(map(request))
				.tracingContext(tracingContext)
				.build();
	}

	static DeactivateCustomerCommand map(UUID id, String reason, TracingContext tracingContext) {
		return DeactivateCustomerCommand.builder()
				.customerId(id)
				.reason(reason)
				.tracingContext(tracingContext)
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
