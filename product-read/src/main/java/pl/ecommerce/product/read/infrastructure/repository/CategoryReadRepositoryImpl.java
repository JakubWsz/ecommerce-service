package pl.ecommerce.product.read.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import pl.ecommerce.product.read.domain.model.CategoryReadModel;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Repository
public class CategoryReadRepositoryImpl implements CategoryReadRepositoryCustom {

	private final ReactiveMongoTemplate mongoTemplate;

	@Override
	public Mono<UpdateResult> updateFirst(Query query, Update update) {
		return mongoTemplate.updateFirst(query, update, CategoryReadModel.class)
				.map(UpdateResult::fromMongoUpdateResult);
	}

	@Override
	public Mono<UpdateResult> updateMulti(Query query, Update update) {
		return mongoTemplate.updateMulti(query, update, CategoryReadModel.class)
				.map(UpdateResult::fromMongoUpdateResult);
	}

	@Override
	public Mono<UpdateResult> incrementProductCount(UUID categoryId, int delta, String traceId) {
		log.debug("Incrementing product count for category: {}, delta: {}, traceId: {}",
				categoryId, delta, traceId);

		Update update = new Update()
				.inc("productCount", delta)
				.set("updatedAt", Instant.now())
				.set("lastTraceId", traceId)
				.set("lastOperation", "UpdateProductCount")
				.set("lastUpdatedAt", Instant.now());

		return updateCategory(categoryId, update, traceId, null);
	}

	@Override
	public Mono<UpdateResult> updateCategory(UUID categoryId, Update update, String traceId) {
		return updateCategory(categoryId, update, traceId, null);
	}

	private Mono<UpdateResult> updateCategory(UUID categoryId, Update update, String traceId, String spanId) {
		if (Objects.nonNull(spanId)) {
			update.set("lastSpanId", spanId);
		}

		Query query = Query.query(Criteria.where("_id").is(categoryId));
		return updateFirst(query, update)
				.doOnSuccess(result -> log.debug("Category updated: {}, modified: {}, traceId: {}",
						categoryId, result.getModifiedCount(), traceId))
				.doOnError(error -> log.error("Error updating category: {}, traceId: {}",
						error.getMessage(), traceId, error));
	}
}