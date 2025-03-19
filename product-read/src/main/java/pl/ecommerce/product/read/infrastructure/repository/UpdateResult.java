package pl.ecommerce.product.read.infrastructure.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResult {
	private long matchedCount;
	private long modifiedCount;
	private boolean acknowledged;

	public static UpdateResult fromMongoUpdateResult(com.mongodb.client.result.UpdateResult result) {
		return new UpdateResult(
				result.getMatchedCount(),
				result.getModifiedCount(),
				result.wasAcknowledged()
		);
	}
}