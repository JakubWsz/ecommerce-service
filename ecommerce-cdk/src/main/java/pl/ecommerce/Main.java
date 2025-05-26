package pl.ecommerce;

import pl.ecommerce.infra.EcommerceStack;
import software.amazon.awscdk.App;

import static java.util.Objects.nonNull;

public class Main {
    private static final String ECOMMERCE_PREFIX = "Ecommerce-";
    private static final String STAGE_LABEL = "stage";
    private static final String DEV_LABEL = "dev";

    public static void main(final String[] args) {
        App app = new App();
        String stage = nonNull(app.getNode().tryGetContext(STAGE_LABEL))
                ? app.getNode().tryGetContext(STAGE_LABEL).toString()
                : DEV_LABEL;

        new EcommerceStack(app, ECOMMERCE_PREFIX + stage, stage);
        app.synth();
    }
}