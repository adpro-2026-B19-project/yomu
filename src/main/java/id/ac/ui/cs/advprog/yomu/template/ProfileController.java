package id.ac.ui.cs.advprog.yomu.template;

import id.ac.ui.cs.advprog.yomu.auth.service.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal userPrincipal) {
            model.addAttribute("loggedInName", userPrincipal.getUsername());
            model.addAttribute("loggedInEmail", userPrincipal.getEmail());
        } else {
            model.addAttribute("loggedInName", authentication.getName());
            model.addAttribute("loggedInEmail", "");
        }
        return "profile/index";
    }
}
