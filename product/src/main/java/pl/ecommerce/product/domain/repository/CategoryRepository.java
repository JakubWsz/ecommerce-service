package pl.ecommerce.product.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.ecommerce.product.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface CategoryRepository extends JpaRepository<Category, UUID> {
	Optional<Category> findByName(String name);

	List<Category> findByParent(Category parent);

	List<Category> findByParentIsNull();

	List<Category> findByActiveTrue();

	@Query("SELECT c FROM Category c JOIN c.products p WHERE p.id = :productId")
	List<Category> findByProductId(@Param("productId") UUID productId);
}
