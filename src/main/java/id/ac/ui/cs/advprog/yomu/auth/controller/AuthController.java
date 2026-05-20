package id.ac.ui.cs.advprog.yomu.auth.controller;

import id.ac.ui.cs.advprog.yomu.auth.dto.LoginForm;
import id.ac.ui.cs.advprog.yomu.auth.dto.RegisterForm;
import id.ac.ui.cs.advprog.yomu.auth.service.AuthService;
import id.ac.ui.cs.advprog.yomu.auth.service.RegisterAttemptService;
import id.ac.ui.cs.advprog.yomu.auth.service.UsernameSuggestionGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
    private final UsernameSuggestionGenerator usernameSuggestionGenerator;
    private final RegisterAttemptService registerAttemptService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public AuthController(
            AuthService authService,
            RegistrationErrorFieldMapper registrationErrorFieldMapper,
            UsernameSuggestionGenerator usernameSuggestionGenerator,
            RegisterAttemptService registerAttemptService,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider
    ) {
        this.authService = authService;
        this.registrationErrorFieldMapper = registrationErrorFieldMapper;
        this.usernameSuggestionGenerator = usernameSuggestionGenerator;
        this.registerAttemptService = registerAttemptService;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    @GetMapping
    public String authPage() {
        return "redirect:/auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm("", "", ""));
        }
        if (!model.containsAttribute("suggestedUsername")) {
            model.addAttribute("suggestedUsername", usernameSuggestionGenerator.generateSuggestion());
        }

        if (!model.containsAttribute("oauthProviders")) {
            model.addAttribute("oauthProviders", resolveOAuthProviders());
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
        if (!model.containsAttribute("oauthProviders")) {
            model.addAttribute("oauthProviders", resolveOAuthProviders());
        }
        return "auth/login";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (registerAttemptService.isLimited(request)) {
            redirectAttributes.addFlashAttribute("warning", "Unable to process registration right now. Please try again later.");
            redirectAttributes.addFlashAttribute("form", new RegisterForm(form.getEmail(), form.getUsername(), ""));
            return "redirect:/auth/register";
        }
        registerAttemptService.recordAttempt(request);

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
        return "redirect:/auth/login";
    }

    private List<String> resolveOAuthProviders() {
        ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (!(clientRegistrationRepository instanceof Iterable<?> registrations)) {
            return List.of();
        }

        List<String> providerIds = new ArrayList<>();
        for (Object registration : registrations) {
            if (registration instanceof ClientRegistration clientRegistration) {
                providerIds.add(clientRegistration.getRegistrationId());
            }
        }
        return providerIds;
    }
}
