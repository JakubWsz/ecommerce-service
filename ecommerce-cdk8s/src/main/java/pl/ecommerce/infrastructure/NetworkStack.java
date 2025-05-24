package pl.ecommerce.infrastructure;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;
import java.util.List;
import java.util.Objects;

public class NetworkStack extends Stack {
    private final Vpc vpc;
    private final List<SecurityGroup> securityGroups;

    public NetworkStack(final Construct scope, final String id, final String stage, final StackProps props) {
        super(scope, id, props);

        this.vpc = Vpc.Builder.create(this, "EcommerceVpc")
                .vpcName(String.format("ecommerce-vpc-%s", stage))
                .maxAzs(3)
                .natGateways(Objects.equals(stage, "prod") ? 3 : 1)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Isolated")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .build();

        this.securityGroups = createSecurityGroups(stage);
    }

    private List<SecurityGroup> createSecurityGroups(String stage) {
        SecurityGroup eksSecurityGroup = SecurityGroup.Builder.create(this, "EksSecurityGroup")
                .vpc(vpc)
                .description("Security group for EKS cluster")
                .securityGroupName(String.format("eks-sg-%s", stage))
                .build();

        SecurityGroup dbSecurityGroup = SecurityGroup.Builder.create(this, "DatabaseSecurityGroup")
                .vpc(vpc)
                .description("Security group for databases")
                .securityGroupName(String.format("db-sg-%s", stage))
                .build();

        dbSecurityGroup.addIngressRule(
                eksSecurityGroup,
                Port.tcp(5432),
                "Allow PostgreSQL from EKS"
        );

        dbSecurityGroup.addIngressRule(
                eksSecurityGroup,
                Port.tcp(27017),
                "Allow MongoDB from EKS"
        );

        return List.of(eksSecurityGroup, dbSecurityGroup);
    }

    public Vpc getVpc() { return vpc; }
    public SecurityGroup getEksSecurityGroup() { return securityGroups.getFirst(); }
    public SecurityGroup getDbSecurityGroup() { return securityGroups.get(1); }
}