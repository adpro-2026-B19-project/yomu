package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameUniquenessServiceTest {

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private UsernameUniquenessService usernameUniquenessService;

    @Test
    void isUsernameTakenByAnotherUserShouldReturnFalseWhenUsernameUnchanged() {
        boolean result = usernameUniquenessService.isUsernameTakenByAnotherUser("alice", "alice");

        assertThat(result).isFalse();
        verify(authRepository, never()).existsByUsername("alice");
    }

    @Test
    void isUsernameTakenByAnotherUserShouldCheckRepositoryWhenUsernameChanged() {
        when(authRepository.existsByUsername("alice-updated")).thenReturn(true);

        boolean result = usernameUniquenessService.isUsernameTakenByAnotherUser("alice-updated", "alice");

        assertThat(result).isTrue();
        verify(authRepository).existsByUsername("alice-updated");
    }
}
