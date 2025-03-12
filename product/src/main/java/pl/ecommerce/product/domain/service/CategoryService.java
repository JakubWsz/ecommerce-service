package pl.ecommerce.product.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ecommerce.product.api.dto.CreateCategoryRequest;
import pl.ecommerce.product.api.dto.UpdateCategoryRequest;
import pl.ecommerce.product.domain.model.Category;
import pl.ecommerce.product.domain.repository.CategoryReactiveRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoryService {

	private final CategoryReactiveRepository categoryRepository;

	@Transactional(readOnly = true)
	public Mono<Category> getCategory(UUID id) {
		log.info("Getting category with id: {}", id);
		return categoryRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Category not found: " + id)));
	}

	@Transactional(readOnly = true)
	public Mono<Category> getCategoryByName(String name) {
		log.info("Getting category with name: {}", name);
		return categoryRepository.findByName(name)
				.switchIfEmpty(Mono.error(new RuntimeException("Category not found with name: " + name)));
	}

	@Transactional(readOnly = true)
	public Flux<Category> getRootCategories() {
		log.info("Getting all root categories");
		return categoryRepository.findRootCategories();
	}

	@Transactional(readOnly = true)
	public Flux<Category> getCategoriesByParentId(UUID parentId) {
		log.info("Getting categories by parent id: {}", parentId);
		return categoryRepository.findByParentId(parentId);
	}

	@Transactional(readOnly = true)
	public Flux<Category> getAllActiveCategories() {
		log.info("Getting all active categories");
		return categoryRepository.findAllActive();
	}

	public Mono<Category> createCategory(CreateCategoryRequest request) {
		log.info("Creating new category: {}", request.name());

		return categoryRepository.findByName(request.name())
				.flatMap(existingCategory -> Mono.<Category>error(new RuntimeException("Category with name already exists: " + request.name())))
				.switchIfEmpty(Mono.defer(() -> createAndSaveCategory(request)));
	}

	public Mono<Category> updateCategory(UUID id, UpdateCategoryRequest request) {
		log.info("Updating category with id: {}", id);

		return categoryRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Category not found: " + id)))
				.flatMap(existingCategory -> updateCategoryFields(existingCategory, request))
				.flatMap(categoryRepository::save);
	}

	public Mono<Void> deleteCategory(UUID id) {
		log.info("Deleting category with id: {}", id);
		return categoryRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Category not found: " + id)))
				.flatMap(category -> {
					if (!category.getProducts().isEmpty()) {
						return Mono.error(new RuntimeException(
								"Cannot delete category with associated products. Deactivate it instead."));
					}
					return categoryRepository.deleteById(id);
				});
	}

	public Mono<Void> deactivateCategory(UUID id) {
		log.info("Deactivating category with id: {}", id);
		return categoryRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Category not found: " + id)))
				.flatMap(category -> {
					category.setActive(false);
					return categoryRepository.save(category)
							.then();
				});
	}

	public Mono<Void> activateCategory(UUID id) {
		log.info("Activating category with id: {}", id);
		return categoryRepository.findById(id)
				.switchIfEmpty(Mono.error(new RuntimeException("Category not found: " + id)))
				.flatMap(category -> {
					category.setActive(true);
					return categoryRepository.save(category)
							.then();
				});
	}

	private Mono<Category> updateCategoryFields(Category existingCategory, UpdateCategoryRequest request) {
		if (nonNull(request.name()) && !request.name().equals(existingCategory.getName())) {
			return categoryRepository.findByName(request.name())
					.flatMap(category -> Mono.<Category>error(new RuntimeException("Category with name already exists: " + request.name())))
					.switchIfEmpty(Mono.defer(() -> {
						existingCategory.setName(request.name());
						return Mono.just(existingCategory);
					}));
		}

		if (nonNull(request.description())) {
			existingCategory.setDescription(request.description());
		}

		if (nonNull(request.active())) {
			existingCategory.setActive(request.active());
		}

		return Mono.just(existingCategory);
	}

	private Mono<Category> createAndSaveCategory(CreateCategoryRequest request) {
		if (request.parentId() != null) {
			return categoryRepository.findById(request.parentId())
					.switchIfEmpty(Mono.error(new RuntimeException("Parent category not found with id: " + request.parentId())))
					.flatMap(parent -> saveCategory(request, parent));
		} else {
			return saveCategory(request, null);
		}
	}

	private Mono<Category> saveCategory(CreateCategoryRequest request, Category parent) {
		Category newCategory = Category.builder()
				.name(request.name())
				.description(request.description())
				.parent(parent)
				.active(true)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
		return categoryRepository.save(newCategory);
	}
}