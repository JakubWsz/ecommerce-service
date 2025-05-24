package pl.ecommerce.k8s;

import org.cdk8s.plus28.k8s.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.Objects;


public class ExternalDnsInstaller {
    static void install(Construct scope) {
        new KubeDeployment(scope, "ExternalDnsDeployment", KubeDeploymentProps.builder()
                .metadata(metadata())
                .spec(deploymentSpec())
                .build());
    }

    private static ObjectMeta metadata() {
        return ObjectMeta.builder()
                .name("external-dns")
                .namespace("kube-system")
                .build();
    }

    private static DeploymentSpec deploymentSpec() {
        return DeploymentSpec.builder()
                .replicas(1)
                .selector(selector())
                .template(template())
                .build();
    }

    private static PodTemplateSpec template() {
        return PodTemplateSpec.builder()
                .metadata(ObjectMeta.builder()
                        .labels(Map.of("app", "external-dns"))
                        .build())
                .spec(podSpec())
                .build();
    }

    private static LabelSelector selector() {
        return LabelSelector.builder()
                .matchLabels(Map.of("app", "external-dns"))
                .build();
    }

    private static PodSpec podSpec() {
        return PodSpec.builder()
                .serviceAccountName("external-dns")
                .containers(containers())
                .build();
    }

    private static List<Container> containers() {
        return List.of(Container.builder()
                .name("external-dns")
                .image("registry.k8s.io/external-dns/external-dns:v0.14.0")
                .args(kubernetesPodArgs())
                .build());
    }

    private static List<String> kubernetesPodArgs() {
        return List.of(
                "--source=service",
                "--source=ingress",
                "--domain-filter=" + Objects.requireNonNullElse(
                        System.getenv("DOMAIN_NAME"),
                        "ecommerce.pl"
                ),
                "--provider=aws",
                "--aws-zone-type=public",
                "--registry=txt",
                "--txt-owner-id=external-dns"
        );
    }
}
