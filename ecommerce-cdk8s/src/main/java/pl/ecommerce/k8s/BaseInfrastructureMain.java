package pl.ecommerce.k8s;

import org.cdk8s.App;
import org.cdk8s.ChartProps;

public class BaseInfrastructureMain {
    public static void main(String[] args) {
        String stage = System.getenv("STAGE") == null ? "dev" : System.getenv("STAGE");
        App app = new App();
        new BaseInfrastructureChart(app, "base-infra-" + stage, stage,
                ChartProps.builder().namespace(stage).build());
        app.synth();
    }
}

