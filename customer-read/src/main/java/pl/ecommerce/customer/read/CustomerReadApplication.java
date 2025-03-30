package pl.ecommerce.customer.read;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"pl.ecommerce.customer.read", "pl.ecommerce.commons"})
public class CustomerReadApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerReadApplication.class, args);
	}
}