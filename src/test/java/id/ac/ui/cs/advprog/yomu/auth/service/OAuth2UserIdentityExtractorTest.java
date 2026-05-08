package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuth2UserIdentityExtractorTest {

    private final OAuth2UserIdentityExtractor extractor = new OAuth2UserIdentityExtractor();

    @Test
    void extractShouldUseProviderFallbackEmailForGithubWhenMissingEmail() {
        OAuth2UserRequest request = new OAuth2UserRequest(
                minimalRegistration("github"),
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token",
                        java.time.Instant.now(),
                        java.time.Instant.now().plusSeconds(60)
                )
        );
        OAuth2User user = new DefaultOAuth2User(java.util.List.of(), Map.of("login", "octo-cat", "name", "Octo Cat"), "login");

        OAuth2UserIdentity identity = extractor.extract(request, user);

        assertThat(identity.email()).isEqualTo("octo-cat@users.noreply.github.com");
        assertThat(identity.preferredUsername()).isEqualTo("octo-cat");
        assertThat(identity.displayName()).isEqualTo("Octo Cat");
    }

    @Test
    void extractShouldThrowWhenEmailMissingForNonGithubProvider() {
        OAuth2UserRequest request = new OAuth2UserRequest(
                minimalRegistration("google"),
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token",
                        java.time.Instant.now(),
                        java.time.Instant.now().plusSeconds(60)
                )
        );
        OAuth2User user = new DefaultOAuth2User(java.util.List.of(), Map.of("name", "No Email"), "name");

        assertThrows(OAuth2AuthenticationException.class, () -> extractor.extract(request, user));
    }

    private ClientRegistration minimalRegistration(String registrationId) {
        Builder builder = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/oauth/userinfo")
                .userNameAttributeName("sub")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        return builder.build();
    }
}
