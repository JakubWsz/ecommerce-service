package pl.ecommerce.product.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.ecommerce.product.domain.model.ProductImage;

import java.util.List;
import java.util.UUID;

@Repository
interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
	List<ProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);

	@Modifying
	@Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
	void deleteByProductId(@Param("productId") UUID productId);
}
