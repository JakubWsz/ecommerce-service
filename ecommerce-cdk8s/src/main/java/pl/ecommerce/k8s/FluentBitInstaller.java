package pl.ecommerce.k8s;

import org.cdk8s.plus28.k8s.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNullElse;

public class FluentBitInstaller {
    public static void install(Construct scope, String stage) {
        new KubeDaemonSet(scope, "FluentBitDaemonSet", props(stage));
    }

    private static KubeDaemonSetProps props(String stage) {
        return KubeDaemonSetProps.builder()
                .metadata(metadata())
                .spec(spec(stage))
                .build();
    }

    private static ObjectMeta metadata() {
        return ObjectMeta.builder()
                .name("fluent-bit")
                .namespace("logging")
                .build();
    }

    private static DaemonSetSpec spec(String stage) {
        return DaemonSetSpec.builder()
                .selector(LabelSelector.builder().matchLabels(Map.of("app", "fluent-bit")).build())
                .template(template(stage))
                .build();
    }

    private static PodTemplateSpec template(String stage) {
        return PodTemplateSpec.builder()
                .metadata(ObjectMeta.builder().labels(Map.of("app", "fluent-bit")).build())
                .spec(podSpec(stage))
                .build();
    }

    private static PodSpec podSpec(String stage) {
        return PodSpec.builder()
                .serviceAccountName("fluent-bit")
                .containers(List.of(createContainer(stage)))
                .volumes(volumes())
                .build();
    }

    private static Container createContainer(String stage) {
        return Container.builder()
                .name("fluent-bit")
                .image("amazon/aws-for-fluent-bit:latest")
                .volumeMounts(volumeMounts())
                .env(env(stage))
                .build();
    }

    private static List<VolumeMount> volumeMounts() {
        return List.of(
                VolumeMount.builder()
                        .name("varlog")
                        .mountPath("/var/log")
                        .readOnly(true)
                        .build(),
                VolumeMount.builder()
                        .name("varlibdockercontainers")
                        .mountPath("/var/lib/docker/containers")
                        .readOnly(true)
                        .build()
        );
    }

    private static List<EnvVar> env(String stage) {
        return List.of(
                EnvVar.builder()
                        .name("AWS_REGION")
                        .value(requireNonNullElse(System.getenv("AWS_REGION"), "eu-west-1"))
                        .build(),
                EnvVar.builder()
                        .name("CLUSTER_NAME")
                        .value(String.format("ecommerce-%s", stage))
                        .build()
        );
    }

    private static List<Volume> volumes() {
        return List.of(
                Volume.builder()
                        .name("varlog")
                        .hostPath(HostPathVolumeSource.builder().path("/var/log").build())
                        .build(),
                Volume.builder()
                        .name("varlibdockercontainers")
                        .hostPath(HostPathVolumeSource.builder().path("/var/lib/docker/containers").build())
                        .build()
        );
    }
}
