package pl.ecommerce.product.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.product.domain.model.ProductImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductImageReactiveRepository {
	private final ProductImageRepository productImageRepository;

	public Mono<ProductImage> findById(UUID id) {
		return Mono.fromCallable(() -> productImageRepository.findById(id).orElse(null))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<ProductImage> findByProductIdOrderBySortOrder(UUID productId) {
		return Mono.fromCallable(() -> productImageRepository.findByProductIdOrderBySortOrderAsc(productId))
				.flatMapMany(Flux::fromIterable)
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<ProductImage> save(ProductImage productImage) {
		return Mono.fromCallable(() -> productImageRepository.save(productImage))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Void> deleteByProductId(UUID productId) {
		return Mono.fromRunnable(() -> productImageRepository.deleteByProductId(productId))
				.subscribeOn(Schedulers.boundedElastic())
				.then();
	}

	public Mono<Void> deletedById(UUID id){
		return Mono.fromRunnable(() -> productImageRepository.deleteById(id))
				.subscribeOn(Schedulers.boundedElastic())
				.then();
	}
}
