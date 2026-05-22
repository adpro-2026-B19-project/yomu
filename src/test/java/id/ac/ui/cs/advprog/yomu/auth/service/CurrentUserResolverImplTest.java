package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

@ExtendWith(MockitoExtension.class)
class CurrentUserResolverImplTest {

    @Mock
    private AuthRepository authRepository;

    @Test
    void resolveUserShouldFindOAuth2UserByEmailAttributeWhenPrincipalIsDefaultOAuth2User() {
        CurrentUserResolverImpl resolver = new CurrentUserResolverImpl(authRepository);
        AuthUser existing = new AuthUser("usertest", "hasanul.muttaqin@ui.ac.id", null, "usertest", "hashed");
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(),
                Map.of("email", "Hasanul.Muttaqin@UI.ac.id", "sub", "google-subject"),
                "sub"
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authRepository.findByEmailAndActiveTrue("hasanul.muttaqin@ui.ac.id")).thenReturn(Optional.of(existing));

        Optional<AuthUser> resolved = resolver.resolveUser(authentication);

        assertThat(resolved).containsSame(existing);
        assertThat(resolver.resolveUsername(authentication)).contains("usertest");
    }
}
