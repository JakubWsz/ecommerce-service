package pl.ecommerce.k8s;

import org.cdk8s.plus28.k8s.KubeNamespace;
import org.cdk8s.plus28.k8s.KubeNamespaceProps;
import org.cdk8s.plus28.k8s.ObjectMeta;
import software.constructs.Construct;

import java.util.Map;

public class NamespaceInstaller {
    static void install(Construct scope, String stage) {

        new KubeNamespace(scope, "MonitoringNs", KubeNamespaceProps.builder()
                .metadata(ObjectMeta.builder()
                        .name("monitoring")
                        .labels(Map.of(
                                "name", "monitoring",
                                "environment", stage
                        ))
                        .build())
                .build());

        new KubeNamespace(scope, "LoggingNs", KubeNamespaceProps.builder()
                .metadata(ObjectMeta.builder()
                        .name("logging")
                        .build())
                .build());

        new KubeNamespace(scope, "CertManagerNs", KubeNamespaceProps.builder()
                .metadata(ObjectMeta.builder()
                        .name("cert-manager")
                        .build())
                .build());
    }
}
