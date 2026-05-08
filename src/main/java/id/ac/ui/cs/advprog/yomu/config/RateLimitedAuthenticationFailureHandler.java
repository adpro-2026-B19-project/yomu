package id.ac.ui.cs.advprog.yomu.config;

import id.ac.ui.cs.advprog.yomu.auth.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class RateLimitedAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String IDENTIFIER_PARAMETER = "identifier";
    private final LoginAttemptService loginAttemptService;

    public RateLimitedAuthenticationFailureHandler(LoginAttemptService loginAttemptService) {
        super("/auth/login?error");
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String identifier = request.getParameter(IDENTIFIER_PARAMETER);
        loginAttemptService.recordFailure(request, identifier);
        super.onAuthenticationFailure(request, response, exception);
    }
}

