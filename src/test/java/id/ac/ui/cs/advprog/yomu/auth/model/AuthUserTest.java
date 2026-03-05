package id.ac.ui.cs.advprog.yomu.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthUserTest {

    @Test
    void prePersistShouldSetCreatedAtWhenNull() {
        AuthUser user = new AuthUser("alice");

        user.prePersist();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getRole()).isEqualTo(AuthRole.USER);
    }

    @Test
    void prePersistShouldNotOverrideExistingCreatedAt() {
        AuthUser user = new AuthUser("alice");
        LocalDateTime fixedTime = LocalDateTime.of(2026, 2, 19, 10, 30);
        ReflectionTestUtils.setField(user, "createdAt", fixedTime);

        user.prePersist();

        assertThat(user.getCreatedAt()).isEqualTo(fixedTime);
    }

    @Test
    void prePersistShouldSetDefaultRoleWhenNull() {
        AuthUser user = new AuthUser("alice");
        ReflectionTestUtils.setField(user, "role", null);

        user.prePersist();

        assertThat(user.getRole()).isEqualTo(AuthRole.USER);
    }
}
