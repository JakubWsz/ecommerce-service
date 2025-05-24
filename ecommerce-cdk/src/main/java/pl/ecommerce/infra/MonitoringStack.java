package pl.ecommerce.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.logs.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Objects;

public class MonitoringStack extends Stack {

    public MonitoringStack(final Construct scope, final String id, final String stage,
                           final StackProps props) {
        super(scope, id, props);

        Bucket logsBucket = Bucket.Builder.create(this, "LogsBucket")
                .bucketName(String.format("ecommerce-logs-%s-%s",
                        Stack.of(this).getAccount(), stage))
                .lifecycleRules(List.of(
                        LifecycleRule.builder()
                                .id("DeleteOldLogs")
                                .expiration(Duration.days(Objects.equals(stage, "prod") ? 90 : 30))
                                .transitions(List.of(
                                        Transition.builder()
                                                .storageClass(StorageClass.INFREQUENT_ACCESS)
                                                .transitionAfter(Duration.days(30))
                                                .build()
                                ))
                                .build()
                ))
                .encryption(BucketEncryption.S3_MANAGED)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        LogGroup applicationLogGroup = LogGroup.Builder.create(this, "ApplicationLogs")
                .logGroupName(String.format("/aws/eks/ecommerce/%s/application", stage))
                .retention(RetentionDays.ONE_MONTH)
                .build();

        new CfnOutput(this, "LogsBucketName", CfnOutputProps.builder()
                .value(logsBucket.getBucketName())
                .exportName(String.format("logs-bucket-%s", stage))
                .build());
    }
}