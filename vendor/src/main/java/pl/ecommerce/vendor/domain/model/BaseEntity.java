package pl.ecommerce.vendor.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

	@Id
	@Builder.Default
	@Field("id")
	private UUID id = UUID.randomUUID();

	@CreatedDate
	@Field("createdAt")
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Field("updatedAt")
	private LocalDateTime updatedAt;

	@CreatedBy
	@Field("createdBy")
	private String createdBy;

	@LastModifiedBy
	@Field("updatedBy")
	private String updatedBy;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BaseEntity that)) return false;

		return id != null && id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}