package pl.ecommerce.k8s;

import org.cdk8s.plus28.k8s.*;
import software.constructs.Construct;

import static pl.ecommerce.k8s.KubeConfigData.kubeConfigData;
import static pl.ecommerce.k8s.KubeStatefulSetSpec.spec;
import static pl.ecommerce.k8s.PrometheusContainerConfig.*;
import static pl.ecommerce.k8s.PrometheusContainerConfig.prometheusContainerResourceRequirements;

public class PrometheusOperatorInstaller {
    static void install(Construct scope) {
        new KubeConfigMap(scope, "PrometheusConfig", KubeConfigMapProps.builder()
                .metadata(kubeConfigMapMetadata())
                .data(kubeConfigData)
                .build());

        Container prometheusContainer = Container.builder()
                .name("prometheus")
                .image("prom/prometheus:v2.47.0")
                .ports(prometheusContainerPorts())
                .args(prometheusContainerArgs())
                .volumeMounts(prometheusContainerVolumeMounts())
                .resources(prometheusContainerResourceRequirements())
                .build();

        new KubeStatefulSet(scope, "PrometheusStatefulSet", KubeStatefulSetProps.builder()
                .metadata(kubeStatefulSetMetadata())
                .spec(spec(prometheusContainer))
                .build());
    }

    private static ObjectMeta kubeStatefulSetMetadata() {
        return ObjectMeta.builder()
                .name("prometheus")
                .namespace("monitoring")
                .build();
    }

    private static ObjectMeta kubeConfigMapMetadata() {
        return ObjectMeta.builder()
                .name("prometheus-config")
                .namespace("monitoring")
                .build();
    }
}
