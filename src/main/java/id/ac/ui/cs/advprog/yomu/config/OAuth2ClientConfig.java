package id.ac.ui.cs.advprog.yomu.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {

    private static final String DEFAULT_GOOGLE_REDIRECT_URI = "{baseUrl}/login/oauth2/code/{registrationId}";
    private static final List<String> DEFAULT_GOOGLE_SCOPES = List.of("email", "profile");

    @Bean
    @Conditional(GoogleOAuth2CredentialsCondition.class)
    public ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        ClientRegistration googleRegistration = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(googleClientId(environment))
                .clientSecret(googleClientSecret(environment))
                .scope(googleScopes(environment))
                .redirectUri(googleRedirectUri(environment))
                .build();

        return new InMemoryClientRegistrationRepository(googleRegistration);
    }

    private static String googleClientId(Environment environment) {
        return firstNonBlank(
                environment.getProperty("GOOGLE_CLIENT_ID"),
                environment.getProperty("app.oauth2.google.client-id"),
                environment.getProperty("spring.security.oauth2.client.registration.google.client-id")
        );
    }

    private static String googleClientSecret(Environment environment) {
        return firstNonBlank(
                environment.getProperty("GOOGLE_CLIENT_SECRET"),
                environment.getProperty("app.oauth2.google.client-secret"),
                environment.getProperty("spring.security.oauth2.client.registration.google.client-secret")
        );
    }

    private static List<String> googleScopes(Environment environment) {
        String configuredScopes = firstNonBlank(
                environment.getProperty("app.oauth2.google.scope"),
                environment.getProperty("spring.security.oauth2.client.registration.google.scope")
        );
        if (configuredScopes.isBlank()) {
            return DEFAULT_GOOGLE_SCOPES;
        }
        return Arrays.stream(configuredScopes.split(","))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .toList();
    }

    private static String googleRedirectUri(Environment environment) {
        return firstNonBlank(
                environment.getProperty("app.oauth2.google.redirect-uri"),
                environment.getProperty("spring.security.oauth2.client.registration.google.redirect-uri"),
                DEFAULT_GOOGLE_REDIRECT_URI
        );
    }

    private static boolean googleOAuthEnabled(Environment environment) {
        return !"false".equalsIgnoreCase(firstNonBlank(
                environment.getProperty("app.oauth2.google.enabled"),
                environment.getProperty("GOOGLE_OAUTH_ENABLED"),
                "true"
        ));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isUsable(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isUsable(String value) {
        if (value == null || value.trim().isBlank()) {
            return false;
        }
        String normalized = value.trim();
        return !normalized.startsWith("ISI_")
                && !normalized.startsWith("your-")
                && !normalized.equalsIgnoreCase("changeme");
    }

    static class GoogleOAuth2CredentialsCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return googleOAuthEnabled(environment)
                    && !googleClientId(environment).isBlank()
                    && !googleClientSecret(environment).isBlank();
        }
    }
}
