package pl.ecommerce.k8s;

import org.cdk8s.plus28.k8s.*;

import java.util.List;
import java.util.Map;

public class KubeStatefulSetSpec {
    static StatefulSetSpec spec(Container prometheusContainer) {
        return StatefulSetSpec.builder()
                .serviceName("prometheus")
                .replicas(1)
                .selector(labelSelector())
                .template(podTemplateSpec(prometheusContainer))
                .volumeClaimTemplates(volumeClaimTemplates())
                .build();
    }

    private static List<KubePersistentVolumeClaimProps> volumeClaimTemplates() {
        return List.of(
                KubePersistentVolumeClaimProps.builder()
                        .metadata(metadata())
                        .spec(persistentVolumeClaimSpec())
                        .build()
        );
    }

    private static PersistentVolumeClaimSpec persistentVolumeClaimSpec() {
        return PersistentVolumeClaimSpec.builder()
                .accessModes(List.of("ReadWriteOnce"))
                .resources(resources())
                .build();
    }

    private static ResourceRequirements resources() {
        return ResourceRequirements.builder()
                .requests(Map.of("storage", Quantity.fromString("10Gi")))
                .build();
    }

    private static ObjectMeta metadata() {
        return ObjectMeta.builder()
                .name("data")
                .build();
    }

    private static PodTemplateSpec podTemplateSpec(Container prometheusContainer) {
        return PodTemplateSpec.builder()
                .metadata(podTemplateSpecMetadata())
                .spec(podSpec(prometheusContainer))
                .build();
    }

    private static PodSpec podSpec(Container prometheusContainer) {
        return PodSpec.builder()
                .serviceAccountName("prometheus")
                .containers(List.of(prometheusContainer))
                .volumes(volumes())
                .build();
    }

    private static List<Volume> volumes() {
        return List.of(
                Volume.builder()
                        .name("config")
                        .configMap(configMapVolumeSource())
                        .build()
        );
    }

    private static ConfigMapVolumeSource configMapVolumeSource() {
        return ConfigMapVolumeSource.builder()
                .name("prometheus-config")
                .build();
    }

    private static ObjectMeta podTemplateSpecMetadata() {
        return ObjectMeta.builder()
                .labels(Map.of("app", "prometheus"))
                .build();
    }

    private static LabelSelector labelSelector() {
        return LabelSelector.builder()
                .matchLabels(Map.of("app", "prometheus"))
                .build();
    }
}
