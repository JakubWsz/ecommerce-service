package pl.ecommerce.infra;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;
import software.amazon.awscdk.services.ec2.Vpc;

public class EcommerceStack extends Stack {
    public EcommerceStack(final Construct scope, final String id, final String stage) {
        super(scope, id, StackProps.builder().build());

        Vpc vpc = VpcFactory.create(this, "EcommerceVpc", stage);
        RdsClusterFactory.create(this, "RdsCluster", vpc, stage);
        DocumentDbFactory.create(this, "DocumentDB", stage);
        CacheFactory.createRedis(this, "Redis");
        MskFactory.create(this, "MSKCluster", stage);
        EksFactory.create(this, "EksCluster", vpc, stage);
        BucketFactory.createLogs(this, "LogsBucket", stage);
        SecretFactory.createDbCreds(this, "DbSecret", stage);
    }
}
