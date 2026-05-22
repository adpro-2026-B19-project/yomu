package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginUserServiceTest {

    @Mock
    private OAuth2UserIdentityExtractor identityExtractor;

    @Mock
    private OAuth2UserProvisioningService provisioningService;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Test
    void loadUserShouldProvisionAndWrapOAuth2UserAsAuthenticatedPrincipal() {
        OAuth2UserRequest request = new OAuth2UserRequest(
                minimalRegistration("google"),
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(), Instant.now().plusSeconds(60))
        );
        OAuth2User providerUser = new DefaultOAuth2User(
                List.of(),
                Map.of("sub", "google-subject", "email", "reader@example.com"),
                "sub"
        );
        OAuth2UserIdentity identity = new OAuth2UserIdentity("google", "reader@example.com", "reader", "Reader");
        AuthUser admin = new AuthUser("reader", "reader@example.com", null, "Reader", null, AuthRole.ADMIN);

        when(delegate.loadUser(request)).thenReturn(providerUser);
        when(identityExtractor.extract(request, providerUser)).thenReturn(identity);
        when(provisioningService.loadOrCreateUser(identity)).thenReturn(admin);

        OAuth2User resolved = new OAuth2LoginUserService(identityExtractor, provisioningService, delegate)
                .loadUser(request);

        assertThat(resolved).isInstanceOf(AuthenticatedOAuth2UserPrincipal.class);
        AuthenticatedOAuth2UserPrincipal principal = (AuthenticatedOAuth2UserPrincipal) resolved;
        assertThat(principal.getUsername()).isEqualTo("reader");
        assertThat(principal.getEmail()).isEqualTo("reader@example.com");
        assertThat(principal.getAttributes()).containsEntry("sub", "google-subject");
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    private ClientRegistration minimalRegistration(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid")
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/oauth/userinfo")
                .userNameAttributeName("sub")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .build();
    }
}
