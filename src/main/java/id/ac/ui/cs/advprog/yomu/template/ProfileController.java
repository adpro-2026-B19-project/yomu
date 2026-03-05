package id.ac.ui.cs.advprog.yomu.template;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.auth.service.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final AuthRepository authRepository;

    public ProfileController(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal userPrincipal) {
            AuthUser user = authRepository.findByEmail(userPrincipal.getEmail())
                    .orElseGet(() -> authRepository.findByUsername(userPrincipal.getUsername()).orElse(null));
            model.addAttribute("user", user);
        } else {
            AuthUser user = authRepository.findByUsername(authentication.getName()).orElse(null);
            model.addAttribute("user", user);
        }
        return "profile/index";
    }
}
