package pl.ecommerce.infra;

import software.amazon.awscdk.services.docdb.CfnDBCluster;
import software.constructs.Construct;

public final class DocumentDbFactory {
    private DocumentDbFactory() {}

    public static void create(Construct scope, String id, String stage) {
         CfnDBCluster.Builder.create(scope, id)
                .masterUsername(EcommerceConstants.ADMIN_USERNAME)
                .masterUserPassword(EcommerceConstants.DEFAULT_PASSWORD)
                .dbClusterIdentifier("ecommerce-docdb-" + stage)
                .build();
    }
}

