package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.config.SecurityThrottleProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final InMemoryRequestRateLimiter rateLimiter;
    private final SecurityThrottleProperties throttleProperties;
    private final ClientAddressResolver clientAddressResolver;

    public LoginAttemptService(
            InMemoryRequestRateLimiter rateLimiter,
            SecurityThrottleProperties throttleProperties,
            ClientAddressResolver clientAddressResolver
    ) {
        this.rateLimiter = rateLimiter;
        this.throttleProperties = throttleProperties;
        this.clientAddressResolver = clientAddressResolver;
    }

    public boolean isLimited(HttpServletRequest request, String identifier) {
        return rateLimiter.isLimited(
                toScopedKey(request, identifier),
                throttleProperties.getLoginFailedMaxAttempts(),
                throttleProperties.getLoginFailedWindowSeconds()
        );
    }

    public boolean isLimitedByIdentifier(String identifier) {
        return rateLimiter.isLimited(
                toIdentifierKey(identifier),
                throttleProperties.getLoginFailedMaxAttempts(),
                throttleProperties.getLoginFailedWindowSeconds()
        );
    }

    public void recordFailure(HttpServletRequest request, String identifier) {
        long windowSeconds = throttleProperties.getLoginFailedWindowSeconds();
        rateLimiter.recordAttempt(toScopedKey(request, identifier), windowSeconds);
        rateLimiter.recordAttempt(toIdentifierKey(identifier), windowSeconds);
    }

    public void clearFailures(HttpServletRequest request, String identifier) {
        rateLimiter.clear(toScopedKey(request, identifier));
        rateLimiter.clear(toIdentifierKey(identifier));
    }

    private String toScopedKey(HttpServletRequest request, String identifier) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim().toLowerCase();
        return "login:" + clientAddressResolver.resolve(request) + ":" + normalizedIdentifier;
    }

    private String toIdentifierKey(String identifier) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim().toLowerCase();
        return "login-id:" + normalizedIdentifier;
    }
}
