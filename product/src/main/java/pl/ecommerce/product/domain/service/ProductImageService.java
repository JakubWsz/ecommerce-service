package pl.ecommerce.product.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.product.domain.model.Product;
import pl.ecommerce.product.domain.model.ProductImage;
import pl.ecommerce.product.domain.repository.ProductImageReactiveRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductImageService {

	private final ProductImageReactiveRepository imageRepository;
	private final ProductService productService;

	@Transactional(readOnly = true)
	public Flux<ProductImage> getImagesByProductId(UUID productId) {
		log.info("Getting images for product: {}", productId);
		return imageRepository.findByProductIdOrderBySortOrder(productId);
	}
	
	public Mono<ProductImage> addProductImage(UUID productId, String imageUrl, Integer sortOrder) {
		log.info("Adding image for product: {}", productId);

		return productService.getProductById(productId)
				.flatMap(product -> {
					ProductImage image = createImage(imageUrl, sortOrder, product);
					return imageRepository.save(image);
				});
	}

	public Mono<Void> deleteProductImage(UUID id) {
		log.info("Deleting image by id: {}", id);

		return imageRepository.deletedById(id);
	}

	public Mono<Void> deleteAllProductImages(UUID productId) {
		log.info("Deleting all images for product: {}", productId);
		return imageRepository.deleteByProductId(productId);
	}

	public Mono<ProductImage> updateImageSortOrder(UUID id, int newSortOrder) {
		log.info("Updating sort order for image: {}", id);

		return imageRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Image not found: " + id)))
				.flatMap(image -> {
					image.setSortOrder(newSortOrder);
					return imageRepository.save(image);
				});
	}

	private static ProductImage createImage(String imageUrl, Integer sortOrder, Product product) {
		return ProductImage.builder()
				.product(product)
				.url(imageUrl)
				.sortOrder(sortOrder != null ? sortOrder : 0)
				.build();
	}
}