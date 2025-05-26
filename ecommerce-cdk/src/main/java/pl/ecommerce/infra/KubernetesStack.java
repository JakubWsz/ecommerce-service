package pl.ecommerce.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.eks.*;
import software.amazon.awscdk.services.iam.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Objects;

public class KubernetesStack extends Stack {
    private final Cluster eksCluster;

    public KubernetesStack(final Construct scope, final String id, final String stage,
                           final NetworkStack networkStack, final StackProps props) {
        super(scope, id, props);

        Role eksRole = Role.Builder.create(this, "EksClusterRole")
                .assumedBy(new ServicePrincipal("eks.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonEKSClusterPolicy"),
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonEKSServicePolicy")
                ))
                .build();

        this.eksCluster = Cluster.Builder.create(this, "EksCluster")
                .clusterName(String.format("ecommerce-%s", stage))
                .version(KubernetesVersion.V1_28)
                .role(eksRole)
                .vpc(networkStack.getVpc())
                .vpcSubnets(List.of(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                        .build()))
                .securityGroup(networkStack.getEksSecurityGroup())
                .defaultCapacity(0)
                .albController(AlbControllerOptions.builder()
                        .version(AlbControllerVersion.V2_6_2)
                        .build())
                .clusterLogging(List.of(
                        ClusterLoggingTypes.API,
                        ClusterLoggingTypes.AUDIT,
                        ClusterLoggingTypes.AUTHENTICATOR,
                        ClusterLoggingTypes.CONTROLLER_MANAGER,
                        ClusterLoggingTypes.SCHEDULER
                ))
                .build();

        eksCluster.addNodegroupCapacity("ManagedNodeGroup", NodegroupOptions.builder()
                .nodegroupName(String.format("ecommerce-nodes-%s", stage))
                .instanceTypes(List.of(
                        new InstanceType("t3.medium"),
                        new InstanceType("t3.large")
                ))
                .minSize(Objects.equals(stage, "prod") ? 3 : 2)
                .maxSize(Objects.equals(stage, "prod") ? 20 : 5)
                .desiredSize(Objects.equals(stage, "prod") ? 5 : 2)
                .diskSize(100)
                .amiType(NodegroupAmiType.AL2_X86_64)
                .build());

        installEksAddons(stage);
        createServiceAccounts();
    }

    private void installEksAddons(String stage) {
        new CfnAddon(this, "VpcCniAddon", CfnAddonProps.builder()
                .addonName("vpc-cni")
                .addonVersion("v1.15.0-eksbuild.2")
                .clusterName(eksCluster.getClusterName())
                .resolveConflicts("OVERWRITE")
                .build());

        new CfnAddon(this, "CoreDnsAddon", CfnAddonProps.builder()
                .addonName("coredns")
                .addonVersion("v1.10.1-eksbuild.6")
                .clusterName(eksCluster.getClusterName())
                .resolveConflicts("OVERWRITE")
                .build());

        new CfnAddon(this, "EbsCsiAddon", CfnAddonProps.builder()
                .addonName("aws-ebs-csi-driver")
                .addonVersion("v1.25.0-eksbuild.1")
                .clusterName(eksCluster.getClusterName())
                .resolveConflicts("OVERWRITE")
                .build());
    }

    private void createServiceAccounts() {
        ServiceAccount fluentBitSa = eksCluster.addServiceAccount("FluentBitSA",
                ServiceAccountOptions.builder()
                        .name("fluent-bit")
                        .namespace("kube-system")
                        .build());

        fluentBitSa.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "logs:CreateLogGroup",
                        "logs:CreateLogStream",
                        "logs:PutLogEvents",
                        "logs:DescribeLogStreams"
                ))
                .resources(List.of("*"))
                .build());

        ServiceAccount externalDnsSa = eksCluster.addServiceAccount("ExternalDnsSA",
                ServiceAccountOptions.builder()
                        .name("external-dns")
                        .namespace("kube-system")
                        .build());

        externalDnsSa.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "route53:ChangeResourceRecordSets",
                        "route53:ListHostedZones",
                        "route53:ListResourceRecordSets"
                ))
                .resources(List.of("*"))
                .build());

        ServiceAccount certManagerSa = eksCluster.addServiceAccount("CertManagerSA",
                ServiceAccountOptions.builder()
                        .name("cert-manager")
                        .namespace("cert-manager")
                        .build());

        certManagerSa.addToPrincipalPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "route53:GetChange",
                        "route53:ChangeResourceRecordSets",
                        "route53:ListHostedZonesByName"
                ))
                .resources(List.of("*"))
                .build());
    }

    public Cluster getEksCluster() { return eksCluster; }
}