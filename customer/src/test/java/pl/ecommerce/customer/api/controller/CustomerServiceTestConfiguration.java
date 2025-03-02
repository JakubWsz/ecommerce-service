package pl.ecommerce.customer.api.controller;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import pl.ecommerce.customer.domain.service.CustomerService;

@TestConfiguration
public class CustomerServiceTestConfiguration {

	@Bean
	public CustomerService customerService() {
		return Mockito.mock(CustomerService.class);
	}
}
