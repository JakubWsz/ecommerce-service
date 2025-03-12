package pl.ecommerce.product.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.product.domain.model.Category;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryReactiveRepository {
	private final CategoryRepository categoryRepository;

	public Mono<Category> findById(UUID id) {
		return Mono.fromCallable(() -> categoryRepository.findById(id).orElse(null))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Category> findByName(String name) {
		return Mono.fromCallable(() -> categoryRepository.findByName(name).orElse(null))
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Category> findByParent(Category parent) {
		return Mono.fromCallable(() -> categoryRepository.findByParent(parent))
				.flatMapMany(Flux::fromIterable)
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Category> findByParentId(UUID parentId) {
		return Mono.fromCallable(() -> {
					Optional<Category> parent = categoryRepository.findById(parentId);
					return parent.map(categoryRepository::findByParent).orElse(List.of());
				})
				.flatMapMany(Flux::fromIterable)
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Category> findRootCategories() {
		return Mono.fromCallable(categoryRepository::findByParentIsNull)
				.flatMapMany(Flux::fromIterable)
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Category> findAllActive() {
		return Mono.fromCallable(categoryRepository::findByActiveTrue)
				.flatMapMany(Flux::fromIterable)
				.subscribeOn(Schedulers.boundedElastic());
	}

	public Flux<Category> findByProductId(UUID productId) {
		return Mono.fromCallable(() -> categoryRepository.findByProductId(productId))
				.flatMapMany(Flux::fromIterable)
				.subscribeOn(Schedulers.boundedElastic());
	}

	@Transactional
	public Mono<Category> save(Category category) {
		return Mono.fromCallable(() -> categoryRepository.save(category))
				.subscribeOn(Schedulers.boundedElastic());
	}

	@Transactional
	public Mono<Void> deleteById(UUID id) {
		return Mono.fromRunnable(() -> categoryRepository.deleteById(id))
				.subscribeOn(Schedulers.boundedElastic())
				.then();
	}
}