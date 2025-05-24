package pl.ecommerce.infra;

import java.util.List;

import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.eks.*;

import software.constructs.Construct;

public final class EksFactory {
    private EksFactory() {}

    public static void create(
            Construct scope, String id,
            IVpc vpc, String stage) {

        Cluster cluster = Cluster.Builder.create(scope, id)
                .clusterName("ecommerce-" + stage)
                .version(EcommerceConstants.K8S_VERSION)
                .vpc(vpc)
                .defaultCapacity(0)
                .build();

        cluster.addNodegroupCapacity(
                EcommerceConstants.NODEGROUP_NAME,
                NodegroupOptions.builder()
                        .instanceTypes(List.of(
                                new InstanceType(EcommerceConstants.INSTANCE_TYPE_MEDIUM),
                                new InstanceType(EcommerceConstants.INSTANCE_TYPE_LARGE)
                        ))
                        .minSize(EcommerceConstants.EKS_MIN_SIZE)
                        .desiredSize(EcommerceConstants.EKS_DESIRED_SIZE)
                        .maxSize(EcommerceConstants.EKS_MAX_SIZE)
                        .build()
        );

        cluster.addServiceAccount(
                "FluentBitSA",
                ServiceAccountOptions.builder()
                        .name(EcommerceConstants.FLUENT_BIT_SA_NAME)
                        .namespace(EcommerceConstants.FLUENT_BIT_NAMESPACE)
                        .build()
        );
    }
}
