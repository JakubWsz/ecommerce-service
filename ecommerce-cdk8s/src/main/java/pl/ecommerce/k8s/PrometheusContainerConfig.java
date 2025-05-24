package pl.ecommerce.k8s;

import org.cdk8s.plus28.k8s.ContainerPort;
import org.cdk8s.plus28.k8s.Quantity;
import org.cdk8s.plus28.k8s.ResourceRequirements;
import org.cdk8s.plus28.k8s.VolumeMount;

import java.util.List;
import java.util.Map;

public class PrometheusContainerConfig {
    static List<String> prometheusContainerArgs() {
        return List.of(
                "--config.file=/etc/prometheus/prometheus.yml",
                "--storage.tsdb.path=/prometheus/",
                "--web.console.libraries=/usr/share/prometheus/console_libraries",
                "--web.console.templates=/usr/share/prometheus/consoles"
        );
    }

    static List<ContainerPort> prometheusContainerPorts() {
        return List.of(
                ContainerPort.builder()
                        .containerPort(9090)
                        .name("web")
                        .protocol("TCP")
                        .build()
        );
    }

    static List<VolumeMount> prometheusContainerVolumeMounts() {
        return List.of(
                VolumeMount.builder()
                        .name("config")
                        .mountPath("/etc/prometheus")
                        .build(),
                VolumeMount.builder()
                        .name("data")
                        .mountPath("/prometheus")
                        .build()
        );
    }


    static ResourceRequirements prometheusContainerResourceRequirements() {
        return ResourceRequirements.builder()
                .requests(Map.of(
                        "memory", Quantity.fromString("512Mi"),
                        "cpu", Quantity.fromString("250m")
                ))
                .limits(Map.of(
                        "memory", Quantity.fromString("2Gi"),
                        "cpu", Quantity.fromString("1000m")
                ))
                .build();
    }
}
