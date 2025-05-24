package pl.ecommerce.infra;


import static java.util.Objects.nonNull;

import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

public final class VpcFactory {
    private VpcFactory() {
    }

    public static Vpc create(Construct scope, String id, String stage) {
        return Vpc.Builder.create(scope, id)
                .vpcName("ecommerce-vpc-" + stage)
                .maxAzs(EcommerceConstants.MAX_AZS)
                .natGateways(natGateways(stage))
                .build();
    }

    private static int natGateways(String stage) {
        return nonNull(stage) && stage.equals(EcommerceConstants.PROD_STAGE)
                ? EcommerceConstants.PROD_NAT_GATEWAYS
                : EcommerceConstants.NON_PROD_NAT_GATEWAYS;
    }
}