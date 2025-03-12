package pl.ecommerce.product.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.ecommerce.product.domain.model.Product;

import java.util.List;
import java.util.UUID;

@Repository
interface ProductRepository extends JpaRepository<Product, UUID> {
	Page<Product> findByActiveTrue(Pageable pageable);

	Page<Product> findByVendorIdAndActiveTrue(UUID vendorId, Pageable pageable);

	@Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId AND p.active = true")
	Page<Product> findByCategoryIdAndActiveTrue(@Param("categoryId") UUID categoryId, Pageable pageable);

	@Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId AND p.vendorId = :vendorId AND p.active = true")
	Page<Product> findByCategoryIdAndVendorIdAndActiveTrue(
			@Param("categoryId") UUID categoryId,
			@Param("vendorId") UUID vendorId,
			Pageable pageable);

	@Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.active = true")
	Page<Product> searchByNameContainingAndActiveTrue(@Param("name") String name, Pageable pageable);

	List<Product> findByIdInAndActiveTrue(List<UUID> ids);

	@Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
	Long countByActiveTrue();

	@Query("SELECT COUNT(p) FROM Product p JOIN p.categories c WHERE c.id = :categoryId AND p.active = true")
	Long countByCategoryIdAndActiveTrue(@Param("categoryId") UUID categoryId);
}