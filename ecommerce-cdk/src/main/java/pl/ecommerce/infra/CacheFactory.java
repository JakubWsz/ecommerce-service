package pl.ecommerce.infra;

import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.constructs.Construct;

public final class CacheFactory {
    private CacheFactory() {}

    public static void createRedis(Construct scope, String id) {
        CfnCacheCluster.Builder.create(scope, id)
                .cacheNodeType(EcommerceConstants.CACHE_NODE_TYPE)
                .engine(EcommerceConstants.CACHE_ENGINE)
                .numCacheNodes(EcommerceConstants.CACHE_NUM_NODES)
                .build();
    }
}

