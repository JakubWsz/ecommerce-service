package pl.ecommerce.customer.domain.repository;

import pl.ecommerce.customer.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerRepository {

	Mono<Customer> saveCustomer(Customer customer);

	Mono<Customer> getCustomerById(UUID id);

	Mono<Customer> updateCustomer(UUID id, Customer customer);

	Mono<Void> deleteCustomer(UUID id);

	Mono<Customer> getCustomerByEmail(String email);

	Flux<Customer> getAllActiveCustomers();
}
