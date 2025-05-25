package pl.ecommerce.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.msk.*;
import software.amazon.awscdk.services.logs.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Objects;

public class MessagingStack extends Stack {

    public MessagingStack(final Construct scope, final String id, final String stage,
                          final NetworkStack networkStack, final StackProps props) {
        super(scope, id, props);

        LogGroup kafkaLogGroup = LogGroup.Builder.create(this, "KafkaLogs")
                .logGroupName(String.format("/aws/msk/ecommerce-%s", stage))
                .retention(RetentionDays.ONE_WEEK)
                .build();

        CfnCluster kafkaCluster = CfnCluster.Builder.create(this, "KafkaCluster")
                .clusterName(String.format("ecommerce-kafka-%s", stage))
                .kafkaVersion("3.5.1")
                .numberOfBrokerNodes(Objects.equals(stage, "prod") ? 3 : 1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType(Objects.equals(stage, "prod") ? "kafka.m5.large" : "kafka.t3.small")
                        .clientSubnets(networkStack.getVpc().getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .toList())
                        .securityGroups(List.of(networkStack.getEksSecurityGroup().getSecurityGroupId()))
                        .storageInfo(CfnCluster.StorageInfoProperty.builder()
                                .ebsStorageInfo(CfnCluster.EBSStorageInfoProperty.builder()
                                        .volumeSize(Objects.equals(stage, "prod") ? 100 : 10)
                                        .build())
                                .build())
                        .build())
                .loggingInfo(CfnCluster.LoggingInfoProperty.builder()
                        .brokerLogs(CfnCluster.BrokerLogsProperty.builder()
                                .cloudWatchLogs(CfnCluster.CloudWatchLogsProperty.builder()
                                        .enabled(true)
                                        .logGroup(kafkaLogGroup.getLogGroupName())
                                        .build())
                                .build())
                        .build())
                .encryptionInfo(CfnCluster.EncryptionInfoProperty.builder()
                        .encryptionInTransit(CfnCluster.EncryptionInTransitProperty.builder()
                                .clientBroker("TLS")
                                .inCluster(true)
                                .build())
                        .build())
                .build();
    }

    public String getKafkaBootstrapServers() {
        return Fn.importValue(String.format("kafka-bootstrap-%s",
                Stack.of(this).getStackName()));
    }
}