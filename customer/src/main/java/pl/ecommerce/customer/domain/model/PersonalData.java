package pl.ecommerce.customer.domain.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalData {
	@Field("email")
	private String email;
	@Field("firstName")
	private String firstName;
	@Field("lastName")
	private String lastName;
	@Field("phoneNumber")
	private String phoneNumber;

	public PersonalData(String email, String firstName, String lastName, String phoneNumber) {
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phoneNumber = phoneNumber;
	}

	public PersonalData(PersonalData other) {
		this.email = other.email;
		this.firstName = other.firstName;
		this.lastName = other.lastName;
		this.phoneNumber = other.phoneNumber;
	}
}
