package pl.ecommerce.customer.read.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonalData {
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;

}
