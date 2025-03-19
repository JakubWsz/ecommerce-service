package pl.ecommerce.product.read.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import pl.ecommerce.commons.tracing.TracingContext;
import pl.ecommerce.product.read.api.dto.ProductSummary;
import pl.ecommerce.product.read.application.mapper.ProductMapper;
import pl.ecommerce.product.read.domain.model.ProductReadModel;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

	private final ReactiveMongoTemplate mongoTemplate;

	/**
	 * Wyszukiwanie zaawansowane z wieloma filtrami i sortowaniem
	 */
	public Mono<Page<ProductSummary>> advancedSearch(
			String searchText,
			List<UUID> categoryIds,
			List<UUID> vendorIds,
			List<String> brandNames,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Boolean onlyDiscounted,
			Boolean inStock,
			String attributeName,
			String attributeValue,
			Boolean featured,
			Pageable pageable,
			TracingContext tracingContext) {

		log.debug("Performing advanced product search, traceId: {}",
				tracingContext != null ? tracingContext.getTraceId() : "unknown");

		// Budowanie kryteriów wyszukiwania
		List<Criteria> criteriaList = new ArrayList<>();

		// Zawsze uwzględniamy tylko aktywne produkty
		criteriaList.add(Criteria.where("status").is("ACTIVE"));

		// Wyszukiwanie pełnotekstowe
		if (searchText != null && !searchText.trim().isEmpty()) {
			// Użycie wyrażenia regularnego dla elastyczności wyszukiwania
			String pattern = ".*" + searchText + ".*";
			criteriaList.add(new Criteria().orOperator(
					Criteria.where("name").regex(pattern, "i"),
					Criteria.where("description").regex(pattern, "i"),
					Criteria.where("sku").regex(pattern, "i"),
					Criteria.where("brandName").regex(pattern, "i"),
					Criteria.where("attributes.value").regex(pattern, "i")
			));
		}

		// Filtrowanie po kategoriach
		if (categoryIds != null && !categoryIds.isEmpty()) {
			criteriaList.add(Criteria.where("categoryIds").in(categoryIds));
		}

		// Filtrowanie po dostawcach
		if (vendorIds != null && !vendorIds.isEmpty()) {
			criteriaList.add(Criteria.where("vendorId").in(vendorIds));
		}

		// Filtrowanie po markach
		if (brandNames != null && !brandNames.isEmpty()) {
			criteriaList.add(Criteria.where("brandName").in(brandNames));
		}

		// Filtrowanie po cenie minimalnej
		if (minPrice != null) {
			criteriaList.add(Criteria.where("price.currentPrice").gte(minPrice));
		}

		// Filtrowanie po cenie maksymalnej
		if (maxPrice != null) {
			criteriaList.add(Criteria.where("price.currentPrice").lte(maxPrice));
		}

		// Tylko produkty przecenione
		if (onlyDiscounted != null && onlyDiscounted) {
			criteriaList.add(Criteria.where("price.discounted").ne(null));
		}

		// Tylko produkty dostępne w magazynie
		if (inStock != null && inStock) {
			criteriaList.add(Criteria.where("stock.available").gt(0));
		}

		// Filtrowanie po atrybutach produktu
		if (attributeName != null && !attributeName.isEmpty()) {
			if (attributeValue != null && !attributeValue.isEmpty()) {
				// Szukamy konkretnej wartości atrybutu
				criteriaList.add(Criteria.where("attributes").elemMatch(
						Criteria.where("name").is(attributeName).and("value").is(attributeValue)
				));
			} else {
				// Szukamy produktów mających atrybut o podanej nazwie
				criteriaList.add(Criteria.where("attributes").elemMatch(
						Criteria.where("name").is(attributeName)
				));
			}
		}

		// Tylko produkty wyróżnione
		if (featured != null) {
			criteriaList.add(Criteria.where("featured").is(featured));
		}

		// Łączymy wszystkie kryteria operatorem AND
		Criteria combinedCriteria = new Criteria();
		if (!criteriaList.isEmpty()) {
			combinedCriteria = combinedCriteria.andOperator(
					criteriaList.toArray(new Criteria[0])
			);
		}

		// Tworzymy zapytanie z połączonymi kryteriami
		Query searchQuery = new Query(combinedCriteria);

		// Dodajemy paginację
		Query countQuery = Query.of(searchQuery).skip(0).limit(0);
		searchQuery.with(pageable);

		// Wykonujemy zapytanie
		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		return mongoTemplate.count(countQuery, ProductReadModel.class)
				.flatMap(count ->
						mongoTemplate.find(searchQuery, ProductReadModel.class)
								.map(product -> {
									ProductSummary summary = ProductMapper.toProductSummary(product);
									summary.setTraceId(traceId);
									return summary;
								})
								.collectList()
								.map(list -> new PageImpl<>(list, pageable, count))
				);
	}

	/**
	 * Wyszukiwanie podobnych produktów na podstawie cech, kategorii i ceny
	 */
	public Mono<List<ProductSummary>> findSimilarProducts(
			UUID productId,
			int limit,
			TracingContext tracingContext) {

		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		log.debug("Finding similar products for product ID: {}, traceId: {}", productId, traceId);

		return mongoTemplate.findById(productId, ProductReadModel.class)
				.flatMap(product -> {
					// Budowanie kryteriów podobieństwa
					List<Criteria> similarityCriteria = new ArrayList<>();

					// Musi być aktywny
					similarityCriteria.add(Criteria.where("status").is("ACTIVE"));

					// Wykluczamy ten sam produkt
					similarityCriteria.add(Criteria.where("_id").ne(productId));

					// Produkty w tych samych kategoriach
					if (!product.getCategoryIds().isEmpty()) {
						similarityCriteria.add(Criteria.where("categoryIds").in(product.getCategoryIds()));
					}

					// Produkty tej samej marki
					if (product.getBrandName() != null && !product.getBrandName().isEmpty()) {
						similarityCriteria.add(Criteria.where("brandName").is(product.getBrandName()));
					}

					// Produkty w podobnym przedziale cenowym (+/- 20%)
					BigDecimal currentPrice = product.getPrice().getCurrentPrice();
					BigDecimal lowerPriceRange = currentPrice.multiply(new BigDecimal("0.8"));
					BigDecimal upperPriceRange = currentPrice.multiply(new BigDecimal("1.2"));

					similarityCriteria.add(
							new Criteria().orOperator(
									Criteria.where("price.regular").gte(lowerPriceRange).lte(upperPriceRange),
									Criteria.where("price.discounted").gte(lowerPriceRange).lte(upperPriceRange)
							)
					);

					// Budowanie zapytania
					Criteria combinedCriteria = new Criteria().andOperator(
							similarityCriteria.toArray(new Criteria[0])
					);
					Query query = new Query(combinedCriteria).limit(limit);

					// Wykonanie zapytania
					return mongoTemplate.find(query, ProductReadModel.class)
							.map(similar -> {
								ProductSummary summary = ProductMapper.toProductSummary(similar);
								summary.setTraceId(traceId);
								return summary;
							})
							.collectList();
				})
				.defaultIfEmpty(Collections.emptyList());
	}

	/**
	 * Wyszukiwanie produktów z określonego przedziału cenowego
	 */
	public Mono<Page<ProductSummary>> findProductsByPriceRange(
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Pageable pageable,
			TracingContext tracingContext) {

		log.debug("Finding products in price range: {} - {}, traceId: {}",
				minPrice, maxPrice, tracingContext != null ? tracingContext.getTraceId() : "unknown");

		Criteria criteria = Criteria.where("status").is("ACTIVE");

		if (minPrice != null) {
			criteria = criteria.and("price.currentPrice").gte(minPrice);
		}

		if (maxPrice != null) {
			criteria = criteria.and("price.currentPrice").lte(maxPrice);
		}

		Query priceQuery = new Query(criteria);
		Query countQuery = Query.of(priceQuery).skip(0).limit(0);
		priceQuery.with(pageable);

		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		return mongoTemplate.count(countQuery, ProductReadModel.class)
				.flatMap(count ->
						mongoTemplate.find(priceQuery, ProductReadModel.class)
								.map(product -> {
									ProductSummary summary = ProductMapper.toProductSummary(product);
									summary.setTraceId(traceId);
									return summary;
								})
								.collectList()
								.map(list -> new PageImpl<>(list, pageable, count))
				);
	}

	/**
	 * Wyszukiwanie produktów na wyprzedaży (z przecenami)
	 */
	public Mono<Page<ProductSummary>> findProductsOnSale(
			Integer minDiscountPercentage,
			Pageable pageable,
			TracingContext tracingContext) {

		log.debug("Finding products on sale with min. discount: {}%, traceId: {}",
				minDiscountPercentage, tracingContext != null ? tracingContext.getTraceId() : "unknown");

		Criteria criteria = Criteria.where("status").is("ACTIVE")
				.and("price.discounted").ne(null);

		if (minDiscountPercentage != null && minDiscountPercentage > 0) {
			// Dodajemy filtr na minimalną wartość procentową rabatu
			criteria = criteria.and("price.discountPercentage").gte(minDiscountPercentage);
		}

		Query saleQuery = new Query(criteria);
		Query countQuery = Query.of(saleQuery).skip(0).limit(0);
		saleQuery.with(pageable);

		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		return mongoTemplate.count(countQuery, ProductReadModel.class)
				.flatMap(count ->
						mongoTemplate.find(saleQuery, ProductReadModel.class)
								.map(product -> {
									ProductSummary summary = ProductMapper.toProductSummary(product);
									summary.setTraceId(traceId);
									return summary;
								})
								.collectList()
								.map(list -> new PageImpl<>(list, pageable, count))
				);
	}

	/**
	 * Wyszukiwanie produktów z konkretnymi atrybutami
	 */
	public Mono<Page<ProductSummary>> findProductsByAttributes(
			Map<String, String> attributeFilters,
			Pageable pageable,
			TracingContext tracingContext) {

		log.debug("Finding products with specific attributes, traceId: {}",
				tracingContext != null ? tracingContext.getTraceId() : "unknown");

		List<Criteria> criteriaList = new ArrayList<>();
		criteriaList.add(Criteria.where("status").is("ACTIVE"));

		// Dodajemy filtr dla każdego atrybutu
		attributeFilters.forEach((attrName, attrValue) ->
				criteriaList.add(Criteria.where("attributes").elemMatch(
						Criteria.where("name").is(attrName).and("value").is(attrValue)
				))
		);

		Criteria combinedCriteria = new Criteria().andOperator(
				criteriaList.toArray(new Criteria[0])
		);

		Query query = new Query(combinedCriteria);
		Query countQuery = Query.of(query).skip(0).limit(0);
		query.with(pageable);

		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		return mongoTemplate.count(countQuery, ProductReadModel.class)
				.flatMap(count ->
						mongoTemplate.find(query, ProductReadModel.class)
								.map(product -> {
									ProductSummary summary = ProductMapper.toProductSummary(product);
									summary.setTraceId(traceId);
									return summary;
								})
								.collectList()
								.map(list -> new PageImpl<>(list, pageable, count))
				);
	}

	/**
	 * Znajdowanie najnowszych produktów w systemie
	 */
	public Mono<List<ProductSummary>> findLatestProducts(
			int limit,
			TracingContext tracingContext) {

		log.debug("Finding latest {} products, traceId: {}",
				limit, tracingContext != null ? tracingContext.getTraceId() : "unknown");

		Query query = new Query(Criteria.where("status").is("ACTIVE"))
				.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
				.limit(limit);

		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		return mongoTemplate.find(query, ProductReadModel.class)
				.map(product -> {
					ProductSummary summary = ProductMapper.toProductSummary(product);
					summary.setTraceId(traceId);
					return summary;
				})
				.collectList();
	}

	/**
	 * Znajdowanie najpopularniejszych produktów
	 * (implementacja bazowa - do rozbudowy np. o dane z systemu analitycznego)
	 */
	public Mono<List<ProductSummary>> findPopularProducts(
			int limit,
			TracingContext tracingContext) {

		log.debug("Finding popular products, traceId: {}",
				tracingContext != null ? tracingContext.getTraceId() : "unknown");

		// W prostej implementacji zwracamy wyróżnione produkty
		// W rzeczywistym systemie można zintegrować to z danymi o sprzedaży,
		// oglądaniach, recenzjach itp.

		Query query = new Query(Criteria.where("status").is("ACTIVE")
				.and("featured").is(true))
				.limit(limit);

		String traceId = tracingContext != null ? tracingContext.getTraceId() : "unknown";
		return mongoTemplate.find(query, ProductReadModel.class)
				.map(product -> {
					ProductSummary summary = ProductMapper.toProductSummary(product);
					summary.setTraceId(traceId);
					return summary;
				})
				.collectList();
	}
}