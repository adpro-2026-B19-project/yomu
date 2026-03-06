package id.ac.ui.cs.advprog.yomu.league.controller;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.league.dto.ClanCreateForm;
import id.ac.ui.cs.advprog.yomu.league.service.ClanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClanController {

    private final ClanService clanService;
    private final CurrentUserResolver currentUserResolver;
    private final AuthRepository authRepository;

    public ClanController(
            ClanService clanService,
            CurrentUserResolver currentUserResolver,
            AuthRepository authRepository
    ) {
        this.clanService = clanService;
        this.currentUserResolver = currentUserResolver;
        this.authRepository = authRepository;
    }

    @GetMapping({"/clans", "/interaction"})
    public String clanListPage(Model model, Authentication authentication) {
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new ClanCreateForm());
        }
        List<ClanService.ClanSummary> clans = clanService.listClans();
        Map<UUID, String> userNamesById = authRepository.findAllById(
                        clans.stream().map(ClanService.ClanSummary::createdByUserId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        AuthUser::getId,
                        user -> {
                            String displayName = user.getDisplayName();
                            if (displayName != null && !displayName.isBlank()) {
                                return displayName;
                            }
                            return user.getUsername();
                        },
                        (left, right) -> left
                ));

        model.addAttribute("clans", clans.stream()
                .map(clan -> new ClanListItem(
                        clan.id(),
                        clan.name(),
                        clan.tier(),
                        clan.memberCount(),
                        userNamesById.getOrDefault(clan.createdByUserId(), "Unknown user")
                ))
                .toList());
        currentUserResolver.resolveUsername(authentication)
                .ifPresent(username -> model.addAttribute("loggedInName", username));
        return "league/clans";
    }

    @PostMapping({"/clans", "/interaction"})
    public String createClan(
            @Valid @ModelAttribute("createForm") ClanCreateForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        Optional<AuthUser> currentUser = currentUserResolver.resolveUser(authentication);
        if (currentUser.isEmpty()) {
            return "redirect:/auth/login";
        }

        if (!bindingResult.hasErrors()) {
            try {
                clanService.createClan(new ClanService.CreateClanRequest(form.getName()), currentUser.get().getId());
            } catch (IllegalArgumentException exception) {
                bindingResult.rejectValue("name", "invalid_name", exception.getMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.createForm",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("createForm", form);
            return "redirect:/clans";
        }

        redirectAttributes.addFlashAttribute("success", "Clan created successfully");
        return "redirect:/clans";
    }

    private record ClanListItem(
            UUID id,
            String name,
            String tier,
            long memberCount,
            String createdByName
    ) {
    }
}
