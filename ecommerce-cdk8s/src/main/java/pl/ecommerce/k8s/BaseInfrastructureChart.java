package pl.ecommerce.k8s;

import org.cdk8s.Chart;
import org.cdk8s.ChartProps;
import org.cdk8s.Include;
import org.cdk8s.IncludeProps;
import org.cdk8s.plus28.k8s.*;
import software.constructs.Construct;

import java.util.Objects;

public class BaseInfrastructureChart extends Chart {

    BaseInfrastructureChart(final Construct scope, final String id,
                            final String stage, final ChartProps props) {
        super(scope, id, props);

        NamespaceInstaller.install(this, stage);
        PrometheusOperatorInstaller.install(scope);
        FluentBitInstaller.install(this, stage);
        installCertManager();
        ExternalDnsInstaller.install(scope);

        if (Objects.isNull(System.getenv("SKIP_ARGOCD"))) {
            installArgoCD();
        }
    }

    private void installCertManager() {
        new Include(this, "CertManagerInstall", IncludeProps.builder()
                .url("https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml")
                .build());
    }

    private void installArgoCD() {
        new KubeNamespace(this, "ArgoCDNs", KubeNamespaceProps.builder()
                .metadata(ObjectMeta.builder()
                        .name("argocd")
                        .build())
                .build());

        new Include(this, "ArgoCDHelm", IncludeProps.builder()
                .url("https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml")
                .build());
    }
}