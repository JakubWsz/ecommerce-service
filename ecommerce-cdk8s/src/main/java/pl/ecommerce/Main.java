package pl.ecommerce;

import org.cdk8s.App;
import org.cdk8s.ChartProps;

import static java.util.Objects.isNull;

public class Main {
    public static void main(final String[] args) {
        String stage = isNull(System.getenv("STAGE")) ? "dev" : System.getenv("STAGE");

        App app = new App();

        new EcommerceChart(
                app,
                String.format("ecommerce-%s", stage),
                stage,
                ChartProps.builder()
                        .namespace(stage)
                        .build()
        );

        app.synth();
    }
}