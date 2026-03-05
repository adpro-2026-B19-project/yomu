package id.ac.ui.cs.advprog.yomu.auth.controller;

import id.ac.ui.cs.advprog.yomu.auth.dto.LoginForm;
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
    private final RegistrationErrorFieldMapper registrationErrorFieldMapper;
    private final LoginErrorFieldMapper loginErrorFieldMapper;

    public AuthController(
            AuthService authService,
            RegistrationErrorFieldMapper registrationErrorFieldMapper,
            LoginErrorFieldMapper loginErrorFieldMapper
    ) {
        this.authService = authService;
        this.registrationErrorFieldMapper = registrationErrorFieldMapper;
        this.loginErrorFieldMapper = loginErrorFieldMapper;
    }

    @GetMapping
    public String authPage(Model model) {
        model.addAttribute("users", authService.findAllUsers());
        model.addAttribute("userCount", authService.countUsers());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm("", "", ""));
        }
        return "auth/index";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm("", "", ""));
        }
        return "auth/register";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm("", ""));
        }
        if (!model.containsAttribute("registeredName")) {
            model.addAttribute("registeredName", "");
        }
        if (!model.containsAttribute("registeredEmail")) {
            model.addAttribute("registeredEmail", "");
        }
        if (!model.containsAttribute("registeredHashedPassword")) {
            model.addAttribute("registeredHashedPassword", "");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginForm") LoginForm loginForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        AuthService.LoginResult loginResult = null;
        if (!bindingResult.hasErrors()) {
            loginResult = authService.loginUser(new AuthService.LoginRequest(loginForm.getEmail(), loginForm.getPassword()));
            if (!loginResult.success()) {
                bindingResult.rejectValue(
                        loginErrorFieldMapper.resolve(loginResult.errorCode()),
                        loginResult.errorCode(),
                        loginResult.errorMessage()
                );
            }
        }

        if (bindingResult.hasErrors()) {
            if (loginResult != null && !loginResult.success()) {
                redirectAttributes.addFlashAttribute("loginWarning", loginResult.errorMessage());
            }
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.loginForm",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("loginForm", loginForm);
            return "redirect:/auth/login";
        }

        AuthService.LoggedInUserSummary loggedInUser = loginResult.loggedInUser();
        redirectAttributes.addFlashAttribute("loggedInName", loggedInUser.username());
        redirectAttributes.addFlashAttribute("loggedInEmail", loggedInUser.email());
        return "redirect:/auth/login";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        AuthService.RegistrationResult registrationResult = null;
        if (!bindingResult.hasErrors()) {
            registrationResult = authService.registerUser(
                    new AuthService.RegisterRequest(form.getEmail(), form.getUsername(), form.getPassword())
            );
            if (!registrationResult.success()) {
                bindingResult.rejectValue(
                        registrationErrorFieldMapper.resolve(registrationResult.errorCode()),
                        registrationResult.errorCode(),
                        registrationResult.errorMessage()
                );
            }
        }

        if (bindingResult.hasErrors()) {
            if (registrationResult != null && !registrationResult.success()) {
                redirectAttributes.addFlashAttribute("warning", registrationResult.errorMessage());
            }
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.form",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/auth/register";
        }

        AuthService.RegisteredUserSummary registeredUser = registrationResult.registeredUser();
        redirectAttributes.addFlashAttribute("registeredName", registeredUser.username());
        redirectAttributes.addFlashAttribute("registeredEmail", registeredUser.email());
        redirectAttributes.addFlashAttribute("registeredHashedPassword", registeredUser.hashedPassword());
        return "redirect:/auth/login";
    }
}
