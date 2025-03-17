package pl.ecommerce.vendor.write;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@ComponentScan(basePackages = {"pl.ecommerce.vendor.write", "pl.ecommerce.commons"})
@EnableKafka
public class VendorWriteApplication {

	public static void main(String[] args) {
		SpringApplication.run(VendorWriteApplication.class, args);
	}
}