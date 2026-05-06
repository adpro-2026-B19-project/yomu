package id.ac.ui.cs.advprog.yomu.league.controller;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.league.dto.ClanCreateForm;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.service.ClanService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        Map<UUID, String> userNamesById = resolveDisplayNames(
                clans.stream().map(ClanService.ClanSummary::createdByUserId).toList()
        );

        model.addAttribute("clans", clans.stream()
                .map(clan -> new ClanListItem(
                        clan.id(),
                        clan.name(),
                        clan.tier(),
                        clan.memberCount(),
                        clan.createdByUserId(),
                        userNamesById.getOrDefault(clan.createdByUserId(), "Unknown user")
                ))
                .toList());
        currentUserResolver.resolveUsername(authentication)
                .ifPresent(username -> model.addAttribute("loggedInName", username));
        return "league/clans";
    }

    @GetMapping("/leaderboard")
    public String leaderboardPage(
            @RequestParam(defaultValue = "BRONZE") String tier,
            Model model,
            Authentication authentication
    ) {
        TierCode selectedTier = parseTierCodeOrDefault(tier);
        List<ClanService.LeaderboardEntry> entries = clanService.getLeaderboard(selectedTier);
        List<ClanService.ClanSummary> clans = clanService.listClans();
        Map<UUID, UUID> clanCreatorById = clans.stream().collect(Collectors.toMap(
                ClanService.ClanSummary::id,
                ClanService.ClanSummary::createdByUserId,
                (left, right) -> left
        ));

        Map<UUID, String> userNamesById = resolveDisplayNames(clanCreatorById.values().stream().toList());
        model.addAttribute("entries", entries.stream()
                .map(entry -> new ClanLeaderboardItem(
                        entry.clanId(),
                        entry.clanName(),
                        entry.tier(),
                        entry.memberCount(),
                        entry.baseScore(),
                        entry.score(),
                        entry.activeModifiers().stream()
                                .map(modifier -> new ScoreModifierView(
                                        modifier.code(),
                                        modifier.label(),
                                        modifier.multiplier(),
                                        modifier.description()
                                ))
                                .toList(),
                        entry.formulaDescription(),
                        clanCreatorById.get(entry.clanId()),
                        userNamesById.getOrDefault(clanCreatorById.get(entry.clanId()), "Unknown user")
                ))
                .toList());
        model.addAttribute("selectedTier", selectedTier.name());
        model.addAttribute("availableTiers", TierCode.values());
        model.addAttribute("tierFormula", describeTier(selectedTier));
        model.addAttribute("adminCanEndSeason", isAdmin(authentication));
        currentUserResolver.resolveUsername(authentication)
                .ifPresent(username -> model.addAttribute("loggedInName", username));
        return "league/leaderboard";
    }

    @GetMapping("/players/{userId}")
    public String publicProfilePage(
            @PathVariable UUID userId,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            ClanService.PublicProfile profile = clanService.getPublicProfile(userId);
            model.addAttribute("publicProfile", new PublicProfileView(
                    profile.userId(),
                    profile.username(),
                    profile.displayName(),
                    profile.role(),
                    profile.clanName(),
                    profile.clanTier(),
                    profile.clanRole(),
                    profile.clanScore(),
                    profile.completedQuizCount(),
                    profile.totalQuizScore(),
                    profile.averageAccuracy(),
                    profile.displayedAchievements().stream()
                            .map(achievement -> new DisplayedAchievementView(
                                    achievement.achievementId(),
                                    achievement.name(),
                                    achievement.milestone(),
                                    achievement.unlockedAt()
                            ))
                            .toList()
            ));
            currentUserResolver.resolveUsername(authentication)
                    .ifPresent(username -> model.addAttribute("loggedInName", username));
            return "league/public-profile";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/clans";
        }
    }

    @GetMapping("/clans/{clanId}")
    public String clanDetailPage(
            @PathVariable UUID clanId,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        Optional<AuthUser> currentUser = currentUserResolver.resolveUser(authentication);
        if (currentUser.isEmpty()) {
            return "redirect:/auth/login";
        }

        try {
            ClanService.ClanDetail detail = clanService.getClanDetail(clanId, currentUser.get().getId());
            List<UUID> userIds = new ArrayList<>();
            userIds.add(detail.createdByUserId());
            userIds.addAll(detail.members().stream().map(ClanService.ClanMemberSummary::userId).toList());
            userIds.addAll(detail.pendingJoinRequests().stream().map(ClanService.JoinRequestSummary::requesterUserId).toList());

            Map<UUID, String> userNamesById = resolveDisplayNames(userIds);

            model.addAttribute("clan", new ClanDetailView(
                    detail.id(),
                    detail.name(),
                    detail.tier(),
                    detail.memberCount(),
                    detail.createdByUserId(),
                    userNamesById.getOrDefault(detail.createdByUserId(), "Unknown user"),
                    detail.viewerIsMember(),
                    detail.viewerIsLeader(),
                    detail.viewerHasPendingRequest(),
                    detail.members().stream()
                            .map(member -> new ClanMemberView(
                                    member.userId(),
                                    userNamesById.getOrDefault(member.userId(), "Unknown user"),
                                    member.role()
                            ))
                            .toList(),
                    detail.pendingJoinRequests().stream()
                            .map(request -> new JoinRequestView(
                                    request.id(),
                                    request.requesterUserId(),
                                    userNamesById.getOrDefault(request.requesterUserId(), "Unknown user"),
                                    request.requestedAt()
                            ))
                            .toList()
            ));
            currentUserResolver.resolveUsername(authentication)
                    .ifPresent(username -> model.addAttribute("loggedInName", username));
            return "league/clan-detail";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/clans";
        }
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

    @PostMapping("/clans/{clanId}/join")
    public String submitJoinRequest(
            @PathVariable UUID clanId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        Optional<AuthUser> currentUser = currentUserResolver.resolveUser(authentication);
        if (currentUser.isEmpty()) {
            return "redirect:/auth/login";
        }

        try {
            clanService.submitJoinRequest(clanId, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Join request has been sent");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/clans/" + clanId;
    }

    @PostMapping("/clans/{clanId}/requests/{joinRequestId}/decision")
    public String reviewJoinRequest(
            @PathVariable UUID clanId,
            @PathVariable UUID joinRequestId,
            @RequestParam("action") String action,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        Optional<AuthUser> currentUser = currentUserResolver.resolveUser(authentication);
        if (currentUser.isEmpty()) {
            return "redirect:/auth/login";
        }

        try {
            clanService.reviewJoinRequest(
                    clanId,
                    joinRequestId,
                    currentUser.get().getId(),
                    toDecision(action)
            );
            redirectAttributes.addFlashAttribute("success", "Join request has been processed");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/clans/" + clanId;
    }

    @PostMapping("/clans/{clanId}/delete")
    public String deleteClan(
            @PathVariable UUID clanId,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        Optional<AuthUser> currentUser = currentUserResolver.resolveUser(authentication);
        if (currentUser.isEmpty()) {
            return "redirect:/auth/login";
        }

        try {
            clanService.deleteClan(clanId, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Clan deleted successfully");
            return "redirect:/clans";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/clans/" + clanId;
        }
    }

    @PostMapping("/admin/league/season/end")
    public String endSeason(RedirectAttributes redirectAttributes) {
        ClanService.SeasonTransitionResult result = clanService.endCurrentSeason();
        redirectAttributes.addFlashAttribute(
                "success",
                "Season %d ended. Season %d started. %d clan movement(s) processed."
                        .formatted(
                                result.endedSeasonNumber(),
                                result.newSeasonNumber(),
                                result.clanTierChanges().size()
                        )
        );
        return "redirect:/leaderboard?tier=BRONZE";
    }

    private Map<UUID, String> resolveDisplayNames(List<UUID> userIds) {
        return authRepository.findAllById(userIds.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(
                        AuthUser::getId,
                        this::toDisplayName,
                        (left, right) -> left
                ));
    }

    private String toDisplayName(AuthUser user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }

    private TierCode parseTierCodeOrDefault(String tier) {
        try {
            return TierCode.valueOf(tier == null ? "BRONZE" : tier.trim().toUpperCase());
        } catch (IllegalArgumentException ignoredInvalidTier) {
            return TierCode.BRONZE;
        }
    }

    private String describeTier(TierCode tierCode) {
        return switch (tierCode) {
            case BRONZE -> "Bronze menghitung total skor kuis seluruh anggota pada season aktif.";
            case SILVER ->
                    "Silver memakai rata-rata tertimbang total skor season per anggota. Anggota aktif berbobot 1.25.";
            case GOLD ->
                    "Gold memakai rata-rata tertimbang total skor season per anggota. Anggota aktif berbobot 1.5.";
            case DIAMOND ->
                    "Diamond memakai formula Gold lalu menambahkan faktor frekuensi aktivitas mingguan dengan cap 25%.";
        };
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private ClanService.JoinRequestDecision toDecision(String action) {
        if ("approve".equalsIgnoreCase(action)) {
            return ClanService.JoinRequestDecision.APPROVE;
        }
        if ("reject".equalsIgnoreCase(action)) {
            return ClanService.JoinRequestDecision.REJECT;
        }
        throw new IllegalArgumentException("Unknown action");
    }

    private record ClanListItem(
            UUID id,
            String name,
            String tier,
            long memberCount,
            UUID createdByUserId,
            String createdByName
    ) {
    }

    private record ClanDetailView(
            UUID id,
            String name,
            String tier,
            long memberCount,
            UUID createdByUserId,
            String createdByName,
            boolean viewerIsMember,
            boolean viewerIsLeader,
            boolean viewerHasPendingRequest,
            List<ClanMemberView> members,
            List<JoinRequestView> pendingJoinRequests
    ) {
    }

    private record ClanMemberView(
            UUID userId,
            String displayName,
            String role
    ) {
    }

    private record JoinRequestView(
            UUID id,
            UUID requesterUserId,
            String requesterDisplayName,
            LocalDateTime requestedAt
    ) {
    }

    private record ClanLeaderboardItem(
            UUID clanId,
            String clanName,
            String tier,
            long memberCount,
            double baseScore,
            double score,
            List<ScoreModifierView> activeModifiers,
            String formulaDescription,
            UUID createdByUserId,
            String createdByName
    ) {
    }

    private record ScoreModifierView(
            String code,
            String label,
            double multiplier,
            String description
    ) {
    }

    private record PublicProfileView(
            UUID userId,
            String username,
            String displayName,
            String role,
            String clanName,
            String clanTier,
            String clanRole,
            double clanScore,
            long completedQuizCount,
            double totalQuizScore,
            double averageAccuracy,
            List<DisplayedAchievementView> displayedAchievements
    ) {
    }

    private record DisplayedAchievementView(
            Long achievementId,
            String name,
            String milestone,
            LocalDateTime unlockedAt
    ) {
    }
}
