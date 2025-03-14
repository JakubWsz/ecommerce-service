package pl.ecommerce.customer.write;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"pl.ecommerce.customer.write", "pl.ecommerce.commons"})
public class CustomerWriteApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerWriteApplication.class, args);
	}
}
