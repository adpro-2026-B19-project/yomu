package id.ac.ui.cs.advprog.yomu.config;

import id.ac.ui.cs.advprog.yomu.auth.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RateLimitedAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final String IDENTIFIER_PARAMETER = "identifier";
    private final LoginAttemptService loginAttemptService;

    public RateLimitedAuthenticationSuccessHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        String identifier = request.getParameter(IDENTIFIER_PARAMETER);
        loginAttemptService.clearFailures(request, identifier);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

