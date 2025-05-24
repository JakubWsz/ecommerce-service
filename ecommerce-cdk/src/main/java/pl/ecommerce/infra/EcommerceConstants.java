package pl.ecommerce.infra;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.eks.KubernetesVersion;

public final class EcommerceConstants {
    private EcommerceConstants() {
    }

    public static final String PROD_STAGE = "prod";

    public static final int MAX_AZS = 3;
    public static final int PROD_NAT_GATEWAYS = 3;
    public static final int NON_PROD_NAT_GATEWAYS = 1;

    public static final String WRITER_INSTANCE_ID = "writer";
    public static final String READER_INSTANCE_ID = "reader1";
    public static final int READER_PROMOTION_TIER = 1;

    public static final String ADMIN_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "changeMe123!";

    public static final String CACHE_ENGINE = "redis";
    public static final String CACHE_NODE_TYPE = "cache.t3.micro";
    public static final int CACHE_NUM_NODES = 1;

    public static final String KAFKA_VERSION = "3.5.1";
    public static final int MSK_BROKER_NODES = 3;
    public static final String MSK_INSTANCE_TYPE = "kafka.t3.small";

    public static final KubernetesVersion K8S_VERSION = KubernetesVersion.V1_28;
    public static final String INSTANCE_TYPE_MEDIUM = "t3.medium";
    public static final String INSTANCE_TYPE_LARGE = "t3.large";
    public static final int EKS_MIN_SIZE = 2;
    public static final int EKS_DESIRED_SIZE = 3;
    public static final int EKS_MAX_SIZE = 10;
    public static final String NODEGROUP_NAME = "ManagedNodeGroup";
    public static final String FLUENT_BIT_SA_NAME = "fluent-bit";
    public static final String FLUENT_BIT_NAMESPACE = "kube-system";

    public static final Duration BUCKET_EXPIRATION = Duration.days(30);

    public static final String SECRET_NAME_TEMPLATE = "ecommerce-db-credentials-%s";
    public static final String SECRET_STRING_TEMPLATE = "{\"username\":\"%s\"}";
    public static final String SECRET_PASSWORD_KEY = "password";
    public static final String SECRET_EXCLUDE_CHARACTERS = " %+~`#$&*()|[]{}:;<>?!'/";
}

