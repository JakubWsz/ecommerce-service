package pl.ecommerce.product.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import pl.ecommerce.product.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductReactiveRepository {
	private final ProductRepository productRepository;

	public Mono<Product> findById(UUID id) {
		return Mono.fromCallable(() -> productRepository.findById(id).orElse(null))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Product> findAllActive(Pageable pageable) {
		return Mono.fromCallable(() -> productRepository.findByActiveTrue(pageable))
				.flatMapMany(page -> Flux.fromIterable(page.getContent()))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Product> findByVendorIdAndActive(UUID vendorId, Pageable pageable) {
		return Mono.fromCallable(() -> productRepository.findByVendorIdAndActiveTrue(vendorId, pageable))
				.flatMapMany(page -> Flux.fromIterable(page.getContent()))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Product> findByCategoryIdAndActive(UUID categoryId, Pageable pageable) {
		return Mono.fromCallable(() -> productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable))
				.flatMapMany(page -> Flux.fromIterable(page.getContent()))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Product> findByCategoryIdAndVendorIdAndActive(UUID categoryId, UUID vendorId, Pageable pageable) {
		return Mono.fromCallable(() ->
						productRepository.findByCategoryIdAndVendorIdAndActiveTrue(categoryId, vendorId, pageable))
				.flatMapMany(page -> Flux.fromIterable(page.getContent()))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Product> searchByNameContainingAndActive(String query, Pageable pageable) {
		return Mono.fromCallable(() -> productRepository.searchByNameContainingAndActiveTrue(query, pageable))
				.flatMapMany(page -> Flux.fromIterable(page.getContent()))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Long> countActive() {
		return Mono.fromCallable(productRepository::countByActiveTrue)
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Long> countByCategoryIdAndActive(UUID categoryId) {
		return Mono.fromCallable(() -> productRepository.countByCategoryIdAndActiveTrue(categoryId))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Product> save(Product product) {
		return Mono.fromCallable(() -> productRepository.save(product))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Void> deleteById(UUID id) {
		return Mono.fromRunnable(() -> productRepository.deleteById(id))
				.subscribeOn(Schedulers.boundedElastic())
				.then();
	}
}