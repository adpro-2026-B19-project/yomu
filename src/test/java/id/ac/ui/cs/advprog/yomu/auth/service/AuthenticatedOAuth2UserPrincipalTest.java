package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuthenticatedOAuth2UserPrincipalTest {

    @Test
    void principalShouldExposeAuthIdentityAndOAuth2Attributes() {
        Map<String, Object> attributes = Map.of("sub", "google-subject", "email", "reader@example.com");
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        AuthenticatedOAuth2UserPrincipal principal = new AuthenticatedOAuth2UserPrincipal(
                "reader",
                "reader@example.com",
                attributes,
                authorities
        );

        assertThat(principal.getName()).isEqualTo("reader");
        assertThat(principal.getUsername()).isEqualTo("reader");
        assertThat(principal.getEmail()).isEqualTo("reader@example.com");
        assertThat(principal.getAttributes()).isSameAs(attributes);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }
}
