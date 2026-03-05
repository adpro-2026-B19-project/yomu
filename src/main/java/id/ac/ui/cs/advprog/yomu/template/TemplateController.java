package id.ac.ui.cs.advprog.yomu.template;

import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TemplateController {

    private final CurrentUserResolver currentUserResolver;

    public TemplateController(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        currentUserResolver.resolveUsername(authentication)
                .ifPresent(username -> model.addAttribute("loggedInName", username));
        return "landingPage";
    }
}
