package pl.ecommerce.customer.write.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ecommerce.commons.model.customer.AddressType;
import pl.ecommerce.commons.model.customer.CustomerConsents;
import pl.ecommerce.commons.model.customer.CustomerPreferences;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;
import pl.ecommerce.customer.write.domain.commands.*;
import pl.ecommerce.customer.write.infrastructure.exception.CustomerAlreadyExistsException;
import pl.ecommerce.customer.write.infrastructure.exception.CustomerNotFoundException;
import pl.ecommerce.customer.write.infrastructure.exception.GdprConsentRequiredException;
import pl.ecommerce.customer.write.infrastructure.repository.CustomerRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {

	@Mock
	private CustomerRepository customerRepository;

	@InjectMocks
	private CustomerApplicationService customerApplicationService;

	@Mock
	private CustomerAggregate mockCustomerAggregate;

	private UUID customerId;

	@BeforeEach
	void setUp() {
		customerId = UUID.randomUUID();
	}

	@Nested
	@DisplayName("Register Customer Tests")
	class RegisterCustomerTests {

		private RegisterCustomerCommand buildValidCommand() {
			return RegisterCustomerCommand.builder()
					.customerId(customerId)
					.email("test@example.com")
					.firstName("John")
					.lastName("Doe")
					.phoneNumber("+1234567890")
					.password("Password123")
					.consents(CustomerConsents.builder()
							.gdprConsent(true)
							.marketingConsent(true)
							.dataProcessingConsent(true)
							.build())
					.build();
		}

		@Test
		@DisplayName("Should register a new customer successfully")
		void shouldRegisterCustomerSuccessfully() {
			RegisterCustomerCommand command = buildValidCommand();
			when(customerRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
			when(customerRepository.save(any(CustomerAggregate.class))).thenReturn(Mono.just(mockCustomerAggregate));

			StepVerifier.create(customerApplicationService.registerCustomer(command))
					.expectNext(mockCustomerAggregate)
					.verifyComplete();

			verify(customerRepository).save(any(CustomerAggregate.class));
		}

		@Test
		@DisplayName("Should throw exception when email already exists")
		void shouldThrowExceptionWhenEmailAlreadyExists() {
			RegisterCustomerCommand command = buildValidCommand();
			when(customerRepository.existsByEmail(command.email())).thenReturn(Mono.just(true));

			StepVerifier.create(customerApplicationService.registerCustomer(command))
					.expectErrorMatches(throwable -> throwable instanceof CustomerAlreadyExistsException &&
							throwable.getMessage().contains("Customer with email already exists"))
					.verify();

			verify(customerRepository, never()).save(any(CustomerAggregate.class));
		}

		@Test
		@DisplayName("Should throw exception when GDPR consent is missing")
		void shouldThrowExceptionWhenGdprConsentIsMissing() {
			CustomerConsents consentsWithoutGdpr = CustomerConsents.builder()
					.gdprConsent(false)
					.marketingConsent(true)
					.dataProcessingConsent(true)
					.build();

			RegisterCustomerCommand command = RegisterCustomerCommand.builder()
					.customerId(customerId)
					.email("test@example.com")
					.firstName("John")
					.lastName("Doe")
					.phoneNumber("+1234567890")
					.password("Password123")
					.consents(consentsWithoutGdpr)
					.build();

			when(customerRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));

			StepVerifier.create(customerApplicationService.registerCustomer(command))
					.expectErrorMatches(throwable -> throwable instanceof GdprConsentRequiredException &&
							throwable.getMessage().contains("GDPR consent is required"))
					.verify();

			verify(customerRepository, never()).save(any(CustomerAggregate.class));
		}

		@Test
		@DisplayName("Should generate UUID when customerId is null")
		void shouldGenerateUuidWhenCustomerIdIsNull() {
			RegisterCustomerCommand command = RegisterCustomerCommand.builder()
					.customerId(null)
					.email("test@example.com")
					.firstName("John")
					.lastName("Doe")
					.phoneNumber("+1234567890")
					.password("Password123")
					.consents(CustomerConsents.builder()
							.gdprConsent(true)
							.marketingConsent(true)
							.dataProcessingConsent(true)
							.build())
					.build();

			when(customerRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
			when(customerRepository.save(any(CustomerAggregate.class))).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.registerCustomer(command))
					.expectNext(mockCustomerAggregate)
					.verifyComplete();

			verify(customerRepository).save(any(CustomerAggregate.class));
		}
	}

	@Nested
	@DisplayName("Update Customer Tests")
	class UpdateCustomerTests {

		@Test
		@DisplayName("Should update customer successfully")
		void shouldUpdateCustomerSuccessfully() {
			UpdateCustomerCommand command = UpdateCustomerCommand.builder()
					.customerId(customerId)
					.firstName("Jane")
					.lastName("Smith")
					.phoneNumber("+9876543210")
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.updateCustomer(command))
					.verifyComplete();

			verify(mockCustomerAggregate).updateBasicInfo(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}

		@Test
		@DisplayName("Should throw exception when customer not found")
		void shouldThrowExceptionWhenCustomerNotFound() {
			UpdateCustomerCommand command = UpdateCustomerCommand.builder()
					.customerId(customerId)
					.firstName("Jane")
					.lastName("Smith")
					.phoneNumber("+9876543210")
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.empty());

			StepVerifier.create(customerApplicationService.updateCustomer(command))
					.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException &&
							throwable.getMessage().contains("Customer not found"))
					.verify();

			verify(mockCustomerAggregate, never()).updateBasicInfo(any());
			verify(customerRepository, never()).save(any(CustomerAggregate.class));
		}
	}

	@Nested
	@DisplayName("Change Email Tests")
	class ChangeEmailTests {

		@Test
		@DisplayName("Should change email successfully")
		void shouldChangeEmailSuccessfully() {
			ChangeCustomerEmailCommand command = ChangeCustomerEmailCommand.builder()
					.customerId(customerId)
					.newEmail("new.email@example.com")
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.changeEmail(command))
					.verifyComplete();

			verify(mockCustomerAggregate).changeEmail(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}
	}

	@Nested
	@DisplayName("Verify Email Tests")
	class VerifyEmailTests {

		@Test
		@DisplayName("Should verify email successfully")
		void shouldVerifyEmailSuccessfully() {
			VerifyCustomerEmailCommand command = VerifyCustomerEmailCommand.builder()
					.customerId(customerId)
					.verificationToken("token123")
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.verifyEmail(command))
					.verifyComplete();

			verify(mockCustomerAggregate).verifyEmail(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}
	}

	@Nested
	@DisplayName("Verify Phone Tests")
	class VerifyPhoneTests {

		@Test
		@DisplayName("Should verify phone number successfully")
		void shouldVerifyPhoneNumberSuccessfully() {
			VerifyCustomerPhoneCommand command = VerifyCustomerPhoneCommand.builder()
					.customerId(customerId)
					.verificationToken("token123")
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.verifyPhoneNumber(command))
					.verifyComplete();

			verify(mockCustomerAggregate).verifyPhoneNumber(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}
	}

	@Nested
	@DisplayName("Shipping Address Tests")
	class ShippingAddressTests {

		@Test
		@DisplayName("Should add shipping address successfully")
		void shouldAddShippingAddressSuccessfully() {
			UUID addressId = UUID.randomUUID();
			AddShippingAddressCommand command = AddShippingAddressCommand.builder()
					.customerId(customerId)
					.addressId(addressId)
					.addressType(AddressType.SHIPPING)
					.street("Main St")
					.buildingNumber("123")
					.city("New York")
					.postalCode("10001")
					.country("USA")
					.isDefault(true)
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.addShippingAddress(command))
					.expectNext(mockCustomerAggregate)
					.verifyComplete();

			verify(mockCustomerAggregate).addShippingAddress(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}

		@Test
		@DisplayName("Should update shipping address successfully")
		void shouldUpdateShippingAddressSuccessfully() {
			UUID addressId = UUID.randomUUID();
			UpdateShippingAddressCommand command = UpdateShippingAddressCommand.builder()
					.customerId(customerId)
					.addressId(addressId)
					.street("Broadway")
					.buildingNumber("456")
					.city("Brooklyn")
					.postalCode("11201")
					.country("USA")
					.isDefault(true)
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.updateShippingAddress(command))
					.expectNext(mockCustomerAggregate)
					.verifyComplete();

			verify(mockCustomerAggregate).updateShippingAddress(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}

		@Test
		@DisplayName("Should remove shipping address successfully")
		void shouldRemoveShippingAddressSuccessfully() {
			UUID addressId = UUID.randomUUID();
			RemoveShippingAddressCommand command = RemoveShippingAddressCommand.builder()
					.customerId(customerId)
					.addressId(addressId)
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.removeShippingAddress(command))
					.verifyComplete();

			verify(mockCustomerAggregate).removeShippingAddress(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}
	}

	@Nested
	@DisplayName("Preferences Tests")
	class PreferencesTests {

		@Test
		@DisplayName("Should update preferences successfully")
		void shouldUpdatePreferencesSuccessfully() {
			CustomerPreferences preferences = CustomerPreferences.builder()
					.marketingConsent(true)
					.newsletterSubscribed(true)
					.preferredLanguage("en")
					.preferredCurrency("USD")
					.favoriteCategories(List.of("electronics", "books"))
					.build();

			UpdateCustomerPreferencesCommand command = UpdateCustomerPreferencesCommand.builder()
					.customerId(customerId)
					.preferences(preferences)
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.updatePreferences(command))
					.verifyComplete();

			verify(mockCustomerAggregate).updatePreferences(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}
	}

	@Nested
	@DisplayName("Account Status Tests")
	class AccountStatusTests {

		@Test
		@DisplayName("Should deactivate customer successfully")
		void shouldDeactivateCustomerSuccessfully() {
			DeactivateCustomerCommand command = DeactivateCustomerCommand.builder()
					.customerId(customerId)
					.reason("Customer requested deactivation")
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));

			StepVerifier.create(customerApplicationService.deactivate(command))
					.verifyComplete();

			verify(mockCustomerAggregate).deactivate(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}

		@Test
		@DisplayName("Should reactivate customer successfully")
		void shouldReactivateCustomerSuccessfully() {
			ReactivateCustomerCommand command = ReactivateCustomerCommand.builder()
					.customerId(customerId)
					.note("Customer requested reactivation")
					.build();

			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.reactivate(command))
					.verifyComplete();

			verify(mockCustomerAggregate).reactivate(command);
			verify(customerRepository).save(mockCustomerAggregate);
		}

		@Test
		@DisplayName("Should delete customer successfully")
		void shouldDeleteCustomerSuccessfully() {
			when(customerRepository.findById(customerId)).thenReturn(Mono.just(mockCustomerAggregate));
			when(customerRepository.save(mockCustomerAggregate)).thenReturn(Mono.just(mockCustomerAggregate));
			

			StepVerifier.create(customerApplicationService.deleteCustomer(customerId))
					.verifyComplete();

			verify(mockCustomerAggregate).delete(any(DeleteCustomerCommand.class));
			verify(customerRepository).save(mockCustomerAggregate);
		}
	}

	@Test
	@DisplayName("Should properly handle customer not found for any command")
	void shouldHandleCustomerNotFound() {
		when(customerRepository.findById(customerId)).thenReturn(Mono.empty());

		ChangeCustomerEmailCommand emailCommand = ChangeCustomerEmailCommand.builder()
				.customerId(customerId)
				.newEmail("test@example.com")
				.build();

		StepVerifier.create(customerApplicationService.changeEmail(emailCommand))
				.expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException)
				.verify();

		verify(customerRepository).findById(customerId);
		verify(customerRepository, never()).save(any(CustomerAggregate.class));
	}
}