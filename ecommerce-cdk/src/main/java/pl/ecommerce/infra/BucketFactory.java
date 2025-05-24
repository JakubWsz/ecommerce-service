package pl.ecommerce.infra;

import java.util.List;

import software.amazon.awscdk.services.s3.*;
import software.constructs.Construct;

public final class BucketFactory {
    private BucketFactory() {
    }

    public static void createLogs(Construct scope, String id, String stage) {
        Bucket.Builder.create(scope, id)
                .bucketName("ecommerce-logs-" + stage)
                .lifecycleRules(List.of(lifecycleRule()))
                .build();
    }

    private static LifecycleRule lifecycleRule() {
        return LifecycleRule.builder()
                .expiration(EcommerceConstants.BUCKET_EXPIRATION)
                .build();
    }
}
