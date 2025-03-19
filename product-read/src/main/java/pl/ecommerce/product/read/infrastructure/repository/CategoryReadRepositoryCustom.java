package pl.ecommerce.product.read.infrastructure.repository;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CategoryReadRepositoryCustom {

	/**
	 * Aktualizuje kategorię zgodnie z podanymi parametrami query i update
	 */
	Mono<UpdateResult> updateFirst(Query query, Update update);

	/**
	 * Aktualizuje wiele kategorii zgodnie z podanymi parametrami
	 */
	Mono<UpdateResult> updateMulti(Query query, Update update);

	/**
	 * Inkrementuje licznik produktów dla kategorii
	 */
	Mono<UpdateResult> incrementProductCount(UUID categoryId, int delta, String traceId);

	/**
	 * Aktualizuje kategorię zgodnie z określonym updatem
	 */
	Mono<UpdateResult> updateCategory(UUID categoryId, Update update, String traceId);
}