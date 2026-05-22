package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import id.ac.ui.cs.advprog.yomu.config.SecurityThrottleProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class LoginAttemptServiceTest {

    @Test
    void loginAttemptServiceShouldRecordLimitAndClearScopedAndIdentifierKeys() {
        SecurityThrottleProperties properties = new SecurityThrottleProperties();
        properties.setLoginFailedMaxAttempts(2);
        properties.setLoginFailedWindowSeconds(300);
        LoginAttemptService service = new LoginAttemptService(
                new InMemoryRequestRateLimiter(),
                properties,
                new ClientAddressResolver()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.50");

        service.recordFailure(request, " Reader@Example.COM ");
        service.recordFailure(request, "reader@example.com");

        assertThat(service.isLimited(request, "reader@example.com")).isTrue();
        assertThat(service.isLimitedByIdentifier("reader@example.com")).isTrue();

        service.clearFailures(request, "reader@example.com");

        assertThat(service.isLimited(request, "reader@example.com")).isFalse();
        assertThat(service.isLimitedByIdentifier("reader@example.com")).isFalse();
    }
}
