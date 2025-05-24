package pl.ecommerce.infrastructure;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.secretsmanager.*;
import software.amazon.awscdk.services.elasticache.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;

public class DatabaseStack extends Stack {
    private final DatabaseCluster postgresCluster;
    private final Secret databaseSecret;
    private final CfnCacheCluster redisCluster;


    public DatabaseStack(final Construct scope, final String id, final String stage,
                         final NetworkStack networkStack, final StackProps props) {
        super(scope, id, props);

        this.databaseSecret = Secret.Builder.create(this, "DatabaseSecret")
                .secretName(String.format("ecommerce/db-credentials/%s", stage))
                .description("Credentials for all databases")
                .generateSecretString(SecretStringGenerator.builder()
                        .secretStringTemplate("{\"username\":\"ecommerce_admin\"}")
                        .generateStringKey("password")
                        .excludeCharacters(" %+~`#$&*()|[]{}:;<>?!'/\"\\")
                        .passwordLength(32)
                        .build())
                .build();

        IClusterInstance writer = ClusterInstance.provisioned(
                "writer",
                ProvisionedClusterInstanceProps.builder()
                        .publiclyAccessible(false)
                        .build()
        );

        List<IClusterInstance> readers =
                nonNull(stage) && stage.equals("prod")
                        ? List.of(clusterInstance())
                        : List.of();

        this.postgresCluster = DatabaseCluster.Builder.create(this, "PostgresCluster")
                .engine(DatabaseClusterEngine.auroraPostgres(
                        AuroraPostgresClusterEngineProps.builder()
                                .version(AuroraPostgresEngineVersion.VER_15_4)
                                .build()
                ))
                .credentials(Credentials.fromSecret(databaseSecret))
                .clusterIdentifier(String.format("ecommerce-postgres-%s", stage))
                .defaultDatabaseName("event_store")
                .vpc(networkStack.getVpc())
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .securityGroups(List.of(networkStack.getDbSecurityGroup()))
                .readers(readers)
                .writer(writer)
                .backup(BackupProps.builder()
                        .retention(Duration.days(Objects.equals(stage, "prod") ? 30 : 7))
                        .build())
                .build();

        this.redisCluster = createRedisCluster(stage, networkStack);

        new CfnOutput(this, "PostgresEndpoint", CfnOutputProps.builder()
                .value(postgresCluster.getClusterEndpoint().getHostname())
                .exportName(String.format("postgres-endpoint-%s", stage))
                .build());
    }

    private CfnCacheCluster createRedisCluster(String stage, NetworkStack networkStack) {
        CfnSubnetGroup redisSubnetGroup = CfnSubnetGroup.Builder.create(this, "RedisSubnetGroup")
                .description("Subnet group for Redis")
                .subnetIds(networkStack.getVpc().getIsolatedSubnets().stream()
                        .map(ISubnet::getSubnetId)
                        .toList())
                .build();

        return CfnCacheCluster.Builder.create(this, "RedisCluster")
                .cacheNodeType(Objects.equals(stage, "prod") ? "cache.r6g.large" : "cache.t3.micro")
                .engine("redis")
                .numCacheNodes(1)
                .cacheSubnetGroupName(redisSubnetGroup.getRef())
                .vpcSecurityGroupIds(List.of(networkStack.getDbSecurityGroup().getSecurityGroupId()))
                .build();
    }

    private static IClusterInstance clusterInstance() {
        return ClusterInstance.provisioned(
                "reader1",
                ProvisionedClusterInstanceProps.builder()
                        .promotionTier(1)
                        .build()
        );
    }

    public DatabaseCluster getPostgresCluster() { return postgresCluster; }
    public Secret getDatabaseSecret() { return databaseSecret; }
}