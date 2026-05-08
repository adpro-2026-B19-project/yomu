package id.ac.ui.cs.advprog.yomu.template;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.auth.service.ProfileService;
import id.ac.ui.cs.advprog.yomu.template.dto.ProfileDeleteForm;
import id.ac.ui.cs.advprog.yomu.template.dto.ProfileUpdateForm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final CurrentUserResolver currentUserResolver;
    private final ProfileService profileService;
    private final ProfileUpdateErrorFieldMapper profileUpdateErrorFieldMapper;

    public ProfileController(
            CurrentUserResolver currentUserResolver,
            ProfileService profileService,
            ProfileUpdateErrorFieldMapper profileUpdateErrorFieldMapper
    ) {
        this.currentUserResolver = currentUserResolver;
        this.profileService = profileService;
        this.profileUpdateErrorFieldMapper = profileUpdateErrorFieldMapper;
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        Optional<AuthUser> userOptional = currentUserResolver.resolveUser(authentication);
        AuthUser user = userOptional.orElse(null);
        model.addAttribute("user", user);

        if (!model.containsAttribute("form")) {
            if (user == null) {
                model.addAttribute("form", new ProfileUpdateForm("", "", null));
            } else {
                model.addAttribute("form", new ProfileUpdateForm(
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getPhoneNumber()
                ));
            }
        }

        return "profile/index";
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/profile/delete")
    public String deleteAccountConfirmation(Model model, Authentication authentication) {
        Optional<AuthUser> userOptional = currentUserResolver.resolveUser(authentication);
        AuthUser user = userOptional.orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("user", user);

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ProfileDeleteForm(""));
        }
        return "profile/delete";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("form") ProfileUpdateForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        Optional<AuthUser> userOptional = currentUserResolver.resolveUser(authentication);
        if (userOptional.isEmpty()) {
            return "redirect:/auth/login";
        }
        AuthUser user = userOptional.get();

        if (!bindingResult.hasErrors()) {
            ProfileService.UpdateProfileResult updateResult = profileService.updateProfile(
                    new ProfileService.UpdateProfileRequest(
                            user.getId(),
                            form.getUsername(),
                            form.getDisplayName(),
                            form.getPhoneNumber()
                    )
            );

            if (!updateResult.success()) {
                bindingResult.rejectValue(
                        profileUpdateErrorFieldMapper.resolve(updateResult.errorCode()),
                        updateResult.errorCode(),
                        updateResult.errorMessage()
                );
                redirectAttributes.addFlashAttribute("warning", updateResult.errorMessage());
            } else {
                redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            }
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.form", bindingResult);
            redirectAttributes.addFlashAttribute("form", form);
        }

        return "redirect:/profile";
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/profile/delete")
    public String deleteOwnAccount(
            @Valid @ModelAttribute("form") ProfileDeleteForm form,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        Optional<AuthUser> userOptional = currentUserResolver.resolveUser(authentication);
        if (userOptional.isEmpty()) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.form", bindingResult);
            redirectAttributes.addFlashAttribute("form", new ProfileDeleteForm(""));
            redirectAttributes.addFlashAttribute("warning", "Unable to delete account. Please check your password and try again.");
            return "redirect:/profile/delete";
        }

        AuthUser user = userOptional.get();
        ProfileService.DeleteAccountResult result = profileService.deleteOwnAccount(
                new ProfileService.DeleteAccountRequest(user.getId(), form.getPassword())
        );

        if (!result.success()) {
            redirectAttributes.addFlashAttribute("form", new ProfileDeleteForm(""));
            redirectAttributes.addFlashAttribute("warning", "Unable to delete account. Please check your password and try again.");
            return "redirect:/profile/delete";
        }

        new SecurityContextLogoutHandler().logout(request, response, authentication);
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return "redirect:/auth/login?deleted";
    }
}
