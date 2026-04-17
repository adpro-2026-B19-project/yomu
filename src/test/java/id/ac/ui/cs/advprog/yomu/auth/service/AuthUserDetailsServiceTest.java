package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuthUserDetailsServiceTest {

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private AuthUserDetailsService authUserDetailsService;

    @Test
    void loadUserByUsernameShouldLookupByEmailWhenIdentifierIsEmail() {
        String hashedPassword = "$2a$10$testHash";
        when(authRepository.findByEmail("reader@example.com"))
                .thenReturn(Optional.of(new AuthUser("reader", "reader@example.com", null, "reader", hashedPassword)));

        UserDetails userDetails = authUserDetailsService.loadUserByUsername("reader@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("reader");
        verify(authRepository).findByEmail("reader@example.com");
        verify(authRepository, never()).findByUsername(anyString());
    }

    @Test
    void loadUserByUsernameShouldLookupByUsernameWhenIdentifierIsUsername() {
        String hashedPassword = "$2a$10$testHash";
        when(authRepository.findByUsername("reader_name-01"))
                .thenReturn(Optional.of(new AuthUser("reader_name-01", "reader@example.com", null, "reader", hashedPassword)));

        UserDetails userDetails = authUserDetailsService.loadUserByUsername("reader_name-01");

        assertThat(userDetails.getUsername()).isEqualTo("reader_name-01");
        verify(authRepository).findByUsername("reader_name-01");
        verify(authRepository, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsernameShouldRejectInvalidIdentifierFormat() {
        assertThrows(UsernameNotFoundException.class, () -> authUserDetailsService.loadUserByUsername("invalid value"));

        verifyNoInteractions(authRepository);
    }
}
