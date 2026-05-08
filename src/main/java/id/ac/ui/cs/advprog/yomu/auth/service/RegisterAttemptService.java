package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.config.SecurityThrottleProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class RegisterAttemptService {

    private final InMemoryRequestRateLimiter rateLimiter;
    private final SecurityThrottleProperties throttleProperties;
    private final ClientAddressResolver clientAddressResolver;

    public RegisterAttemptService(
            InMemoryRequestRateLimiter rateLimiter,
            SecurityThrottleProperties throttleProperties,
            ClientAddressResolver clientAddressResolver
    ) {
        this.rateLimiter = rateLimiter;
        this.throttleProperties = throttleProperties;
        this.clientAddressResolver = clientAddressResolver;
    }

    public boolean isLimited(HttpServletRequest request) {
        return rateLimiter.isLimited(
                toKey(request),
                throttleProperties.getRegisterMaxAttempts(),
                throttleProperties.getRegisterWindowSeconds()
        );
    }

    public void recordAttempt(HttpServletRequest request) {
        rateLimiter.recordAttempt(toKey(request), throttleProperties.getRegisterWindowSeconds());
    }

    private String toKey(HttpServletRequest request) {
        return "register:" + clientAddressResolver.resolve(request);
    }
}

