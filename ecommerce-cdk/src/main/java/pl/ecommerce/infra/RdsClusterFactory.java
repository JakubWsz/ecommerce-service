package pl.ecommerce.infra;

import static java.util.Objects.nonNull;

import java.util.List;

import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

public final class RdsClusterFactory {
    private RdsClusterFactory() {
    }

    public static void create(
            Construct scope, String id, IVpc vpc, String stage) {

        IClusterInstance writer = ClusterInstance.provisioned(
                EcommerceConstants.WRITER_INSTANCE_ID,
                ProvisionedClusterInstanceProps.builder()
                        .publiclyAccessible(false)
                        .build()
        );

        List<IClusterInstance> readers =
                nonNull(stage) && stage.equals(EcommerceConstants.PROD_STAGE)
                        ? List.of(clusterInstance())
                        : List.of();

        DatabaseCluster.Builder.create(scope, id)
                .engine(DatabaseClusterEngine.auroraPostgres(pgProps()))
                .writer(writer)
                .readers(readers)
                .vpc(vpc)
                .build();
    }

    private static AuroraPostgresClusterEngineProps pgProps() {
        return AuroraPostgresClusterEngineProps.builder()
                .version(AuroraPostgresEngineVersion.VER_15_4)
                .build();
    }

    private static IClusterInstance clusterInstance() {
        return ClusterInstance.provisioned(
                EcommerceConstants.READER_INSTANCE_ID,
                ProvisionedClusterInstanceProps.builder()
                        .promotionTier(EcommerceConstants.READER_PROMOTION_TIER)
                        .build()
        );
    }
}