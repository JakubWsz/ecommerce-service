package pl.ecommerce;

import org.cdk8s.ChartProps;
import software.constructs.Construct;
import org.cdk8s.Chart;

import static java.util.Objects.isNull;

public class EcommerceChart extends Chart {
    public EcommerceChart(final Construct scope,
                          final String id,
                          final String stage,
                          final ChartProps props) {
        super(scope, id, props);

        String ecrUri = System.getenv("ECR_URI");
        String tag = isNull(System.getenv("TAG")) ? "latest" : System.getenv("TAG");
        String baseImage = ecrUri + ":" + tag;
        int replicas = isNull(System.getenv("REPLICAS")) ? 2 : Integer.parseInt(System.getenv("REPLICAS"));

        new MicroserviceConstruct(this, "CustomerRead", "customer-read", baseImage, replicas, 8080, 80);
        new MicroserviceConstruct(this, "CustomerWrite", "customer-write", baseImage, replicas, 8080, 80);

        new MicroserviceConstruct(this, "PostgresExporter", "postgres-exporter", "prometheuscommunity/postgres-exporter:latest", 1, 9187, 9187);
        new MicroserviceConstruct(this, "RedisExporter", "redis-exporter", "oliver006/redis_exporter:latest", 1, 9121, 9121);
        new MicroserviceConstruct(this, "MongoExporter", "mongodb-exporter", "percona/mongodb_exporter:latest", 1, 9216, 9216);
        new MicroserviceConstruct(this, "KafkaExporter", "kafka-exporter", "danielqsj/kafka-exporter:latest", 1, 9308, 9308);
        new MicroserviceConstruct(this, "NodeExporter", "node-exporter", "prom/node-exporter:latest", 1, 9100, 9100);

        new MicroserviceConstruct(this, "PgAdmin", "pgadmin", "dpage/pgadmin4:latest", 1, 80, 80);
        new MicroserviceConstruct(this, "MongoExpress", "mongo-express", "mongo-express:latest", 1, 8081, 8081);
        new MicroserviceConstruct(this, "RedisCommander", "redis-commander", "rediscommander/redis-commander:latest", 1, 8081, 8081);
        new MicroserviceConstruct(this, "RedisInsight", "redis-insight", "redislabs/redisinsight:latest", 1, 8001, 8001);
        new MicroserviceConstruct(this, "AKHQ", "akhq", "tchiotludo/akhq:latest", 1, 9000, 9000);
        new MicroserviceConstruct(this, "Grafana", "grafana", "grafana/grafana:10.4.0", 1, 3000, 3000);
        new MicroserviceConstruct(this, "Prometheus", "prometheus", "prom/prometheus:v2.49.1", 1, 9090, 9090);
        new MicroserviceConstruct(this, "Alertmanager", "alertmanager", "prom/alertmanager:v0.26.0", 1, 9093, 9093);

        new MicroserviceConstruct(this, "OtelCollector", "otel-collector", "otel/opentelemetry-collector-contrib:latest", 1, 8889, 8889);
        new MicroserviceConstruct(this, "Jaeger", "jaeger", "jaegertracing/all-in-one:latest", 1, 16686, 16686);
    }
}