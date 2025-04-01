package pl.ecommerce.product.read.application.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.CategoryResponse;
import pl.ecommerce.product.read.api.dto.CategorySummary;
import pl.ecommerce.product.read.application.mapper.CategoryMapper;
import pl.ecommerce.product.read.domain.model.CategoryReadModel;
import pl.ecommerce.product.read.infrastructure.repository.CategoryReadRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryQueryService {

	private final CategoryReadRepository categoryRepository;
	private final ObservationRegistry observationRegistry;

	public Mono<CategoryResponse> findById(UUID categoryId, TracingContext tracingContext) {
		log.debug("Finding category by ID: {}, traceId: {}", categoryId, getTraceId(tracingContext));

		return observe("category.findById",
				categoryRepository.findById(categoryId)
						.flatMap(category -> enrichCategoryResponse(category, tracingContext)));
	}

	public Mono<CategoryResponse> findBySlug(String slug, TracingContext tracingContext) {
		log.debug("Finding category by slug: {}, traceId: {}", slug, getTraceId(tracingContext));

		return observe("category.findBySlug",
				categoryRepository.findBySlug(slug)
						.flatMap(category -> enrichCategoryResponse(category, tracingContext)));
	}

	public Mono<Flux<CategorySummary>> getRootCategories(TracingContext tracingContext) {
		log.debug("Finding root categories, traceId: {}", getTraceId(tracingContext));

		return observe("category.getRoots",
				Mono.just(categoryRepository.findByParentCategoryIdIsNull()
						.map(CategoryMapper::toCategorySummary)
						.doOnNext(dto -> dto.setTraceId(getTraceId(tracingContext)))));
	}

	public Mono<Flux<CategorySummary>> getSubcategories(UUID categoryId, TracingContext tracingContext) {
		log.debug("Finding subcategories for category: {}, traceId: {}", categoryId, getTraceId(tracingContext));

		return observe("category.getSubcategories",
				Mono.just(categoryRepository.findByParentCategoryId(categoryId)
						.map(CategoryMapper::toCategorySummary)
						.doOnNext(dto -> dto.setTraceId(getTraceId(tracingContext)))));
	}

	public Mono<Flux<CategoryResponse>> getCategoryTree(TracingContext tracingContext) {
		log.debug("Building category tree, traceId: {}", getTraceId(tracingContext));

		return observe("category.getTree",
				Mono.just(buildCategoryTree(tracingContext)));
	}

	public Mono<Flux<CategorySummary>> searchCategories(String query, TracingContext tracingContext) {
		log.debug("Searching categories with query: {}, traceId: {}", query, getTraceId(tracingContext));

		String searchTerm = ".*" + query + ".*";

		return observe("category.search",
				Mono.just(categoryRepository.searchByName(searchTerm)
						.map(CategoryMapper::toCategorySummary)
						.doOnNext(dto -> dto.setTraceId(getTraceId(tracingContext)))));
	}

	private Flux<CategoryResponse> buildCategoryTree(TracingContext tracingContext) {
		return categoryRepository.findByParentCategoryIdIsNull()
				.flatMap(rootCategory -> buildCategoryBranch(rootCategory, tracingContext));
	}

	private Mono<CategoryResponse> buildCategoryBranch(CategoryReadModel category, TracingContext tracingContext) {
		return categoryRepository.findByParentCategoryId(category.getId())
				.map(CategoryMapper::toCategorySummary)
				.collectList()
				.map(subcategories -> {
					CategoryResponse response = CategoryMapper.toCategoryResponse(category);
					response.setTraceId(getTraceId(tracingContext));
					response.setChildren(subcategories.stream()
							.peek(summary -> summary.setTraceId(getTraceId(tracingContext)))
							.collect(Collectors.toList()));
					return response;
				});
	}

	private Mono<CategoryResponse> enrichCategoryResponse(CategoryReadModel category, TracingContext tracingContext) {
		CategoryResponse response = CategoryMapper.toCategoryResponse(category);
		response.setTraceId(getTraceId(tracingContext));

		if (category.hasSubcategories()) {
			return categoryRepository.findByParentCategoryId(category.getId())
					.map(CategoryMapper::toCategorySummary)
					.collectList()
					.map(subcategories -> {
						response.setChildren(subcategories.stream()
								.peek(summary -> summary.setTraceId(getTraceId(tracingContext)))
								.collect(Collectors.toList()));
						return response;
					});
		}

		return Mono.just(response);
	}

	private <T> Mono<T> observe(String opName, Mono<T> mono) {
		return Observation.createNotStarted(opName, observationRegistry)
				.observe(() -> mono);
	}

	private String getTraceId(TracingContext tracingContext) {
		return tracingContext != null ? tracingContext.getTraceId() : "unknown";
	}
}
