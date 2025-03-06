//package pl.ecommerce.vendor.domain.repository;
//
//import org.springframework.stereotype.Repository;
//import pl.ecommerce.vendor.domain.model.Vendor;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//@Repository
//public class VendorRepository2 {
//
//	private final JdbcTemplate jdbcTemplate;
//
//	public VendorRepository(JdbcTemplate jdbcTemplate) {
//		this.jdbcTemplate = jdbcTemplate;
//	}
//
//	private final RowMapper<Vendor> vendorRowMapper = (rs, rowNum) -> Vendor.builder()
//			.id(UUID.fromString(rs.getString("id")))
//			.name(rs.getString("name"))
//			.description(rs.getString("description"))
//			.email(rs.getString("email"))
//			.phone(rs.getString("phone"))
//			.businessName(rs.getString("business_name"))
//			.taxId(rs.getString("tax_id"))
//			.vendorStatus(Vendor.VendorStatus.valueOf(rs.getString("vendor_status")))
//			.verificationVendorStatus(Vendor.VendorVerificationStatus.valueOf(rs.getString("verification_vendor_status")))
//			.active(rs.getBoolean("active"))
//			.build();
//
//	public Optional<Vendor> findByEmail(String email) {
//		String sql = "SELECT * FROM vendors WHERE email = ?";
//		return jdbcTemplate.query(sql, vendorRowMapper, email).stream().findFirst();
//	}
//
//	public boolean existsByEmail(String email) {
//		String sql = "SELECT COUNT(*) FROM vendors WHERE email = ?";
//		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
//		return count != null && count > 0;
//	}
//
//	public List<Vendor> findByStatus(String status) {
//		String sql = "SELECT * FROM vendors WHERE vendor_status = ?";
//		return jdbcTemplate.query(sql, vendorRowMapper, status);
//	}
//
//	public List<Vendor> findByVerificationStatus(String verificationStatus) {
//		String sql = "SELECT * FROM vendors WHERE verification_vendor_status = ?";
//		return jdbcTemplate.query(sql, vendorRowMapper, verificationStatus);
//	}
//
//	public List<Vendor> findByActiveTrue() {
//		String sql = "SELECT * FROM vendors WHERE active = true";
//		return jdbcTemplate.query(sql, vendorRowMapper);
//	}
//
//	public List<Vendor> findByActiveTrueAndStatus(String status) {
//		String sql = "SELECT * FROM vendors WHERE active = true AND vendor_status = ?";
//		return jdbcTemplate.query(sql, vendorRowMapper, status);
//	}
//
//	public List<Vendor> findByNameContainingIgnoreCase(String nameTerm) {
//		String sql = "SELECT * FROM vendors WHERE LOWER(name) LIKE LOWER(?)";
//		return jdbcTemplate.query(sql, vendorRowMapper, "%" + nameTerm + "%");
//	}
//
//	public List<Vendor> findByBusinessNameContainingIgnoreCase(String businessNameTerm) {
//		String sql = "SELECT * FROM vendors WHERE LOWER(business_name) LIKE LOWER(?)";
//		return jdbcTemplate.query(sql, vendorRowMapper, "%" + businessNameTerm + "%");
//	}
//	public void save(Vendor vendor) {
//		String sql = "INSERT INTO vendors (id, name, description, email, phone, business_name, tax_id, vendor_status, verification_vendor_status, active) " +
//				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
//				"ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), phone = VALUES(phone), " +
//				"business_name = VALUES(business_name), tax_id = VALUES(tax_id), vendor_status = VALUES(vendor_status), " +
//				"verification_vendor_status = VALUES(verification_vendor_status), active = VALUES(active)";
//		jdbcTemplate.update(sql, vendor.getId(), vendor.getName(), vendor.getDescription(), vendor.getEmail(), vendor.getPhone(),
//				vendor.getBusinessName(), vendor.getTaxId(), vendor.getVendorStatus().name(), vendor.getVerificationVendorStatus().name(),
//				vendor.getActive());
//	}
//}
