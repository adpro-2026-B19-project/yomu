package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Test
    void resolveUserShouldReturnEmptyForNullUnauthenticatedAndAnonymousAuthentication() {
        CurrentUserResolverImpl resolver = new CurrentUserResolverImpl(authRepository);
        UsernamePasswordAuthenticationToken unauthenticated =
                UsernamePasswordAuthenticationToken.unauthenticated("reader", "password");
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        assertThat(resolver.resolveUser(null)).isEmpty();
        assertThat(resolver.resolveUsername(null)).isEmpty();
        assertThat(resolver.resolveUser(unauthenticated)).isEmpty();
        assertThat(resolver.resolveUsername(unauthenticated)).isEmpty();
        assertThat(resolver.resolveUser(anonymous)).isEmpty();
        assertThat(resolver.resolveUsername(anonymous)).isEmpty();
    }

    @Test
    void resolveUserShouldFallBackFromPrincipalEmailToUsername() {
        CurrentUserResolverImpl resolver = new CurrentUserResolverImpl(authRepository);
        AuthUser existing = new AuthUser("reader", "reader@example.com", null, "Reader", "hashed", AuthRole.USER);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                "reader",
                "reader@example.com",
                "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authRepository.findByEmailAndActiveTrue("reader@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByUsernameAndActiveTrue("reader")).thenReturn(Optional.of(existing));

        assertThat(resolver.resolveUser(authentication)).containsSame(existing);
        assertThat(resolver.resolveUsername(authentication)).contains("reader");
    }

    @Test
    void resolveUserShouldUseAuthenticationNameForPlainPrincipal() {
        CurrentUserResolverImpl resolver = new CurrentUserResolverImpl(authRepository);
        AuthUser existing = new AuthUser("reader", "reader@example.com", null, "Reader", "hashed", AuthRole.USER);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("reader", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authRepository.findByUsernameAndActiveTrue("reader")).thenReturn(Optional.of(existing));

        assertThat(resolver.resolveUser(authentication)).containsSame(existing);
        assertThat(resolver.resolveUsername(authentication)).contains("reader");
    }

    @Test
    void resolveOAuth2UserWithBlankEmailShouldFallBackToAuthenticationName() {
        CurrentUserResolverImpl resolver = new CurrentUserResolverImpl(authRepository);
        AuthUser existing = new AuthUser("subject", "subject@example.com", null, "Subject", "hashed", AuthRole.USER);
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "   ", "sub", "subject"),
                "sub"
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authRepository.findByUsernameAndActiveTrue("subject")).thenReturn(Optional.of(existing));

        assertThat(resolver.resolveUser(authentication)).containsSame(existing);
        assertThat(resolver.resolveUsername(authentication)).contains("subject");
    }
}
