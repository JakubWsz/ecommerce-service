package pl.ecommerce.k8s;

import java.util.Map;

public class KubeConfigData {
    private static final String KEY = "prometheus.yml";
    private static final String VALUE = """
            global:
              scrape_interval: 15s
              evaluation_interval: 15s
            
            scrape_configs:
              - job_name: 'kubernetes-pods'
                kubernetes_sd_configs:
                  - role: pod
                relabel_configs:
                  - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
                    action: keep
                    regex: true
                  - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
                    action: replace
                    target_label: __metrics_path__
                    regex: (.+)
            """;
    static Map<String, String> kubeConfigData = Map.of(
            KEY, VALUE
    );
}
