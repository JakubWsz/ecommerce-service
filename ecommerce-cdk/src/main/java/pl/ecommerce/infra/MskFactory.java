package pl.ecommerce.infra;

import software.amazon.awscdk.services.msk.CfnCluster;
import software.constructs.Construct;

public final class MskFactory {
    private MskFactory() {
    }

    public static void create(Construct scope, String id, String stage) {
        CfnCluster.Builder.create(scope, id)
                .clusterName("ecommerce-kafka-" + stage)
                .kafkaVersion(EcommerceConstants.KAFKA_VERSION)
                .numberOfBrokerNodes(EcommerceConstants.MSK_BROKER_NODES)
                .brokerNodeGroupInfo(brokerNodeGroupInfo())
                .build();
    }

    private static CfnCluster.BrokerNodeGroupInfoProperty brokerNodeGroupInfo() {
        return CfnCluster.BrokerNodeGroupInfoProperty.builder()
                .instanceType(EcommerceConstants.MSK_INSTANCE_TYPE)
                .build();
    }
}

