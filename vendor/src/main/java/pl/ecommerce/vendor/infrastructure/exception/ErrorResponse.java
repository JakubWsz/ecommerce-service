package pl.ecommerce.vendor.infrastructure.exception;

import java.time.LocalDateTime;

public record ErrorResponse(String message,
							String errorCode,
							LocalDateTime timestamp,
							String details
) {
}
