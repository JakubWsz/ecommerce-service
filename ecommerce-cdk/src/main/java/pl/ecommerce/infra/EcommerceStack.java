package pl.ecommerce.infra;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public class EcommerceStack extends Stack {
    public EcommerceStack(final Construct scope, final String id, final String stage) {
        super(scope, id, StackProps.builder().build());

        NetworkStack networkStack = new NetworkStack(
                this,
                "NetworkStack",
                stage,
                StackProps.builder().build()
        );

        new DatabaseStack(
                this,
                "DatabaseStack",
                stage,
                networkStack,
                StackProps.builder().build()
        );

       new KubernetesStack(
                this,
                "KubernetesStack",
                stage,
                networkStack,
                StackProps.builder().build()
        );

        new MessagingStack(
                this,
                "MessagingStack",
                stage,
                networkStack,
                StackProps.builder().build()
        );

       new MonitoringStack(
                this,
                "MonitoringStack",
                stage,
                StackProps.builder().build()
        );
    }
}
