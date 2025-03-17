package pl.ecommerce.vendor.read.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.ecommerce.commons.model.vendor.VendorStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vendors")
public class VendorReadModel {
	@Id
	private UUID id;
	private String name;
	private String businessName;
	private String taxId;
	private String email;
	private String phone;
	private String legalForm;
	private VendorStatus status;
	private boolean verified;
	private BigDecimal commissionRate;
	private String contactPersonName;
	private String contactPersonEmail;
	private Instant createdAt;
	private Instant updatedAt;
	private BankDetails bankDetails;
	private List<CategoryAssignment> categories = new ArrayList<>();
	private Address address;
	private Map<String, String> metadata = new HashMap<>();
	private String lastTraceId;
	private String lastSpanId;
	private String lastOperation;
	private Instant lastUpdatedAt;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CategoryAssignment {
		private UUID categoryId;
		private String categoryName;
		private Instant assignedAt;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Address {
		private String street;
		private String buildingNumber;
		private String city;
		private String postalCode;
		private String country;
		private String state;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class BankDetails {
		private String accountNumber;
		private String bankName;
		private String swiftCode;
	}
}