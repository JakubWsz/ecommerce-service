package pl.ecommerce.infra;

import software.amazon.awscdk.services.secretsmanager.*;
import software.constructs.Construct;

public final class SecretFactory {
    private SecretFactory() {
    }

    public static void createDbCreds(Construct scope, String id, String stage) {
        Secret.Builder.create(scope, id)
                .secretName(secretName(stage))
                .generateSecretString(secret())
                .build();
    }

    private static SecretStringGenerator secret() {
        return SecretStringGenerator.builder()
                .secretStringTemplate(secretTemplate())
                .generateStringKey(EcommerceConstants.SECRET_PASSWORD_KEY)
                .excludeCharacters(EcommerceConstants.SECRET_EXCLUDE_CHARACTERS)
                .build();
    }

    private static String secretTemplate() {
        return String.format(
                EcommerceConstants.SECRET_STRING_TEMPLATE,
                EcommerceConstants.ADMIN_USERNAME
        );
    }

    private static String secretName(String stage) {
        return String.format(
                EcommerceConstants.SECRET_NAME_TEMPLATE,
                stage
        );
    }
}
