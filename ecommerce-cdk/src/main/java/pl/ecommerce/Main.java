package pl.ecommerce;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import pl.ecommerce.infra.*;

import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        App app = new App();

        String account = app.getNode().tryGetContext("account").toString();
        String stage = Objects.toString(app.getNode().tryGetContext("stage"), "dev");
        String region = System.getenv("AWS_REGION");

        if (Objects.isNull(region) || region.isEmpty()) {
            region = "eu-central-1";
        }

        Environment env = Environment.builder()
                .account(account)
                .region(region)
                .build();

        StackProps stackProps = StackProps.builder()
                .env(env)
                .build();

        NetworkStack networkStack = new NetworkStack(app, "ecommerce-network-" + stage, stage, stackProps);

        DatabaseStack databaseStack = new DatabaseStack(app, "ecommerce-database-" + stage, stage, networkStack, stackProps);
        databaseStack.addDependency(networkStack);

        MessagingStack messagingStack = new MessagingStack(app, "ecommerce-messaging-" + stage, stage, networkStack, stackProps);
        messagingStack.addDependency(networkStack);

        KubernetesStack kubernetesStack = new KubernetesStack(app, "ecommerce-kubernetes-" + stage, stage, networkStack, stackProps);
        kubernetesStack.addDependency(networkStack);

        MonitoringStack monitoringStack = new MonitoringStack(app, "ecommerce-monitoring-" + stage, stage, stackProps);
        monitoringStack.addDependency(networkStack);

        app.synth();
    }
}