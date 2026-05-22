package id.ac.ui.cs.advprog.yomu.template;

import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalNavigationControllerAdvice {

    private final CurrentUserResolver currentUserResolver;

    public GlobalNavigationControllerAdvice(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @ModelAttribute("loggedInName")
    public String loggedInName(Authentication authentication) {
        return currentUserResolver.resolveUsername(authentication).orElse(null);
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(Authentication authentication) {
        return currentUserResolver.resolveUsername(authentication).isPresent();
    }

    @ModelAttribute("profileNavHref")
    public String profileNavHref(Authentication authentication) {
        return currentUserResolver.resolveUsername(authentication).isPresent()
                ? "/profile"
                : "/auth/login";
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
