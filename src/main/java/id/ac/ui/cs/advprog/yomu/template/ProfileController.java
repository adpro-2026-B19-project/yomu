package id.ac.ui.cs.advprog.yomu.template;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    @GetMapping("/profile")
    public String profile(Model model) {
        if (!model.containsAttribute("loggedInName")) {
            model.addAttribute("loggedInName", "");
        }
        if (!model.containsAttribute("loggedInEmail")) {
            model.addAttribute("loggedInEmail", "");
        }
        return "profile/index";
    }
}
