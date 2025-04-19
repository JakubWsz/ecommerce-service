package pl.ecommerce.customer.write.api.mapper;

import pl.ecommerce.customer.write.api.dto.*;
import pl.ecommerce.customer.write.domain.aggregate.CustomerAggregate;

public interface ResponseMapper {

	static CustomerRegistrationResponse map(CustomerAggregate customerAggregate) {
		return new CustomerRegistrationResponse(
				customerAggregate.getId(),
				customerAggregate.getEmail(),
				customerAggregate.getFirstName(),
				customerAggregate.getLastName()
		);
	}
}
