package id.ac.ui.cs.advprog.yomu.template;

import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final CurrentUserResolver currentUserResolver;

    public ProfileController(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        model.addAttribute("user", currentUserResolver.resolveUser(authentication).orElse(null));
        return "profile/index";
    }
}
