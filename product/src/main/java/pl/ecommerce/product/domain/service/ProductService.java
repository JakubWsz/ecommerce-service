package pl.ecommerce.product.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.commons.event.product.ProductCreatedEvent;
import pl.ecommerce.commons.event.product.ProductDeletedEvent;
import pl.ecommerce.commons.event.product.ProductUpdatedEvent;
import pl.ecommerce.commons.kafka.EventPublisher;
import pl.ecommerce.product.api.dto.ProductCreateRequest;
import pl.ecommerce.product.api.dto.ProductUpdateRequest;
import pl.ecommerce.product.domain.model.Category;
import pl.ecommerce.product.domain.model.Product;
import pl.ecommerce.product.domain.model.ProductImage;
import pl.ecommerce.product.domain.repository.ProductReactiveRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {
	private final ProductReactiveRepository productRepository;
	private final EventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public Mono<Product> getProductById(UUID id) {
		log.info("Getting product with id: {}", id);
		return productRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Product not found: " + id)));
	}

	@Transactional(readOnly = true)
	public Flux<Product> getAllActiveProducts(Pageable pageable) {
		log.info("Getting all active products, page: {}", pageable.getPageNumber());
		return productRepository.findAllActive(pageable);
	}

	@Transactional(readOnly = true)
	public Flux<Product> getProductsByVendor(String vendorId, Pageable pageable) {
		log.info("Getting products by vendor: {}", vendorId);
		return productRepository.findByVendorIdAndActive(UUID.fromString(vendorId), pageable);
	}

	@Transactional(readOnly = true)
	public Flux<Product> getProductsByCategory(UUID categoryId, Pageable pageable) {
		log.info("Getting products by category: {}", categoryId);
		return productRepository.findByCategoryIdAndActive(categoryId, pageable);
	}

	@Transactional(readOnly = true)
	public Flux<Product> searchProducts(String query, Pageable pageable) {
		log.info("Searching products with query: {}", query);
		return productRepository.searchByNameContainingAndActive(query, pageable);
	}

	@Transactional(readOnly = true)
	public Mono<Long> countActiveProducts() {
		return productRepository.countActive();
	}

	@Transactional(readOnly = true)
	public Mono<Long> countProductsByCategory(UUID categoryId) {
		return productRepository.countByCategoryIdAndActive(categoryId);
	}

	public Mono<Product> createProduct(ProductCreateRequest request) {
		log.info("Creating new product: {}", request.name());

		Product product = Product.create(
				request.name(),
				request.description(),
				request.price(),
				request.stock(),
				request.vendorId(),
				request.attributes()
		);

		return productRepository.save(product)
				.doOnSuccess(savedProduct -> {
					log.info("Product successfully created with ID: {}", savedProduct.getId());

					ProductCreatedEvent event = createProductCreatedEvent(savedProduct);

					eventPublisher.publish(event);
				});
	}

	public Mono<Product> updateProduct(UUID id, ProductUpdateRequest request) {
		log.info("Updating product with id: {}", id);

		return productRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Product not found: " + id)))
				.flatMap(existingProduct -> updateProduct(request, existingProduct));
	}

	public Mono<Product> updateProductStock(UUID id, int newStock) {
		log.info("Updating stock for product with id: {}", id);
		return productRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Product not found: " + id)))
				.flatMap(product -> {
					Map<String, Object> changes = Map.of("stock",
							Map.of("old", product.getStock(), "new", newStock));
					product.setStock(newStock);
					return productRepository.save(product)
							.doOnSuccess(savedProduct -> publishProductUpdatedEvent(savedProduct, changes));
				});
	}

	public Mono<Product> deactivateProduct(UUID id) {
		return updateProductStatus(id, false, "Deactivating");
	}

	public Mono<Product> activateProduct(UUID id) {
		return updateProductStatus(id, true, "Activating");
	}

	public Mono<Void> deleteProduct(UUID id) {
		log.info("Deleting product with id: {}", id);
		return productRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Product not found: " + id)))
				.flatMap(product -> {

					eventPublisher.publish(createProductDeletedEvent(product));
					return productRepository.deleteById(id);
				});
	}

	private Mono<Product> updateProductStatus(UUID id, boolean active, String action) {
		log.info("{} product with id: {}", action, id);
		return productRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Product not found: " + id)))
				.flatMap(product -> {
					Map<String, Object> changes = Map.of("active", Map.of("old", product.isActive(), "new", active));
					product.setActive(active);
					return productRepository.save(product)
							.doOnSuccess(savedProduct -> publishProductUpdatedEvent(savedProduct, changes));
				});
	}

	private Mono<Product> updateProduct(ProductUpdateRequest updatedProduct, Product existingProduct) {
		Map<String, Object> changes = new HashMap<>();
		applyProductUpdates(existingProduct, updatedProduct, changes);
		return productRepository.save(existingProduct)
				.doOnSuccess(savedProduct -> publishProductUpdatedEvent(savedProduct, changes));
	}

	private void publishProductUpdatedEvent(Product savedProduct, Map<String, Object> changes) {
		log.info("Product successfully updated with ID: {}", savedProduct.getId());
		eventPublisher.publish(createProductUpdatedEvent(savedProduct, changes));
	}

	private void applyProductUpdates(Product existingProduct, ProductUpdateRequest updatedProduct, Map<String, Object> changes) {
		updateFieldIfPresent(updatedProduct.name(), Product::setName, changes, "name", existingProduct);
		updateFieldIfPresent(updatedProduct.description(), Product::setDescription, changes, "description", existingProduct);
		updateFieldIfPresent(updatedProduct.price(), Product::setPrice, changes, "price", existingProduct);
		updateFieldIfPresent(updatedProduct.stock(), Product::setStock, changes, "stock", existingProduct);
		updateFieldIfPresent(updatedProduct.active(), Product::setActive, changes, "active", existingProduct);

		if (updatedProduct.attributes() != null) {
			Map<String, String> oldAttributes = new HashMap<>(existingProduct.getAttributes());
			existingProduct.getAttributes().clear();
			existingProduct.getAttributes().putAll(updatedProduct.attributes());
			changes.put("attributes", Map.of("old", oldAttributes, "new", new HashMap<>(existingProduct.getAttributes())));
		}
	}

	private static ProductDeletedEvent createProductDeletedEvent(Product product) {
		return ProductDeletedEvent.builder()
				.correlationId(UUID.randomUUID())
				.productName(product.getName())
				.vendorId(product.getVendorId())
				.productId(product.getId())
				.build();
	}

	private static ProductUpdatedEvent createProductUpdatedEvent(Product savedProduct, Map<String, Object> changes) {
		return ProductUpdatedEvent.builder()
				.correlationId(UUID.randomUUID())
				.productId(savedProduct.getId())
				.vendorId(savedProduct.getVendorId())
				.productName(savedProduct.getName())
				.description(savedProduct.getDescription())
				.price(savedProduct.getPrice())
				.categories(savedProduct.getCategories().stream().map(Category::getId).toList())
				.stockQuantity(savedProduct.getStock())
				.attributes(savedProduct.getAttributes())
				.updatedAt(savedProduct.getUpdatedAt())
				.images(savedProduct.getImages().stream().map(ProductImage::getUrl).toList())
				.changedFields(changes)
				.build();
	}

	private static ProductCreatedEvent createProductCreatedEvent(Product savedProduct) {
		return ProductCreatedEvent.builder()
				.correlationId(UUID.randomUUID())
				.productId(savedProduct.getId())
				.vendorId(savedProduct.getVendorId())
				.productName(savedProduct.getName())
				.description(savedProduct.getDescription())
				.price(savedProduct.getPrice())
				.categories(savedProduct.getCategories().stream().map(Category::getId).toList())
				.stockQuantity(savedProduct.getStock())
				.attributes(savedProduct.getAttributes())
				.createdAt(savedProduct.getCreatedAt())
				.images(savedProduct.getImages().stream().map(ProductImage::getUrl).toList())
				.build();
	}

	private static <T> void updateFieldIfPresent(T newValue, BiConsumer<Product, T> setter, Map<String, Object> changes,
												 String fieldName, Product existingProduct) {
		if (newValue != null) {
			setter.accept(existingProduct, newValue);
			changes.put(fieldName, newValue);
		}
	}
}