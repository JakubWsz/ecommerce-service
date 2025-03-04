package pl.ecommerce.customer.infrastructure.exception;

import java.time.LocalDateTime;

public record ErrorResponse(String message,
							String errorCode,
							LocalDateTime timestamp,
							String details
) {
}
