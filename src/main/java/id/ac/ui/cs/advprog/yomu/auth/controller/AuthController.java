package id.ac.ui.cs.advprog.yomu.auth.controller;

import id.ac.ui.cs.advprog.yomu.auth.dto.RegisterForm;
import id.ac.ui.cs.advprog.yomu.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public String authPage(Model model) {
        model.addAttribute("users", authService.findAllUsers());
        model.addAttribute("userCount", authService.countUsers());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm(""));
        }
        return "auth/index";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (!bindingResult.hasErrors()) {
            AuthService.RegistrationResult registrationResult = authService.registerUser(form.getUsername());
            if (!registrationResult.success()) {
                bindingResult.rejectValue("username", registrationResult.errorCode(), registrationResult.errorMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.form",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/auth";
        }

        redirectAttributes.addFlashAttribute("message", "User persisted successfully");
        return "redirect:/auth";
    }
}
