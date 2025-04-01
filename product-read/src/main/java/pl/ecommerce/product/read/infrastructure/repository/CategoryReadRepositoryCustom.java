package pl.ecommerce.product.read.infrastructure.repository;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CategoryReadRepositoryCustom {

	Mono<UpdateResult> updateFirst(Query query, Update update);

	Mono<UpdateResult> updateMulti(Query query, Update update);

	Mono<UpdateResult> incrementProductCount(UUID categoryId, int delta, String traceId);

	Mono<UpdateResult> updateCategory(UUID categoryId, Update update, String traceId);
}