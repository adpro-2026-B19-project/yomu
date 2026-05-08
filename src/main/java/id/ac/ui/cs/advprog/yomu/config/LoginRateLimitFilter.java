package id.ac.ui.cs.advprog.yomu.config;

import id.ac.ui.cs.advprog.yomu.auth.service.LoginAttemptService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String IDENTIFIER_PARAMETER = "identifier";

    private final LoginAttemptService loginAttemptService;
    private final SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler("/auth/login?error");

    public LoginRateLimitFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isLoginRequest(request)) {
            String identifier = request.getParameter(IDENTIFIER_PARAMETER);
            if (loginAttemptService.isLimited(request, identifier)) {
                failureHandler.onAuthenticationFailure(
                        request,
                        response,
                        new org.springframework.security.authentication.BadCredentialsException("Invalid credentials")
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return LOGIN_PATH.equals(request.getServletPath()) && "POST".equalsIgnoreCase(request.getMethod());
    }
}

