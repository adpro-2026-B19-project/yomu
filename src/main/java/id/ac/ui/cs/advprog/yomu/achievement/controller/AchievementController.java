package id.ac.ui.cs.advprog.yomu.achievement.controller;

import id.ac.ui.cs.advprog.yomu.achievement.service.DailyMissionService;
import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import id.ac.ui.cs.advprog.yomu.achievement.dto.AchievementCreateForm;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/achievements")
public class AchievementController {

    private static final Logger log = LoggerFactory.getLogger(AchievementController.class);

    private final AchievementService achievementService;
    private final DailyMissionService dailyMissionService;
    private final CurrentUserResolver currentUserResolver;
    private final CategoryRepository categoryRepository;

    // hymeleaf Page

    @GetMapping
    public String achievementListPage(Model model) {
        long start = System.nanoTime();
        List<Achievement> achievements = achievementService.getAllAchievements();
        model.addAttribute("achievements", achievements);
        List<Category> categories = categoryRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
        model.addAttribute("categories", categories);
        Map<Long, String> categoryNamesById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean adminView = isAdmin(auth);
        model.addAttribute("adminView", adminView);
        if (adminView) {
            model.addAttribute("achievementDistribution", achievementService.getAchievementDistribution());
        }
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            model.addAttribute("loggedInName", auth.getName());
            model.addAttribute("todayMissionCards", List.of());
            model.addAttribute("achievementProgressCards", List.of());
            currentUserResolver.resolveUser(auth).ifPresent(user -> {
                var userProgress = dailyMissionService.getUserProgress(user.getId());
                Map<Long, id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress> progressByMissionId = userProgress
                        .stream()
                        .collect(Collectors.toMap(progress -> progress.getMission().getId(), progress -> progress));
                List<DailyMissionCardView> todayMissionCards = dailyMissionService.getTodayMissions().stream()
                        .map(mission -> toDailyMissionCardView(mission, progressByMissionId.get(mission.getId()), categoryNamesById))
                        .toList();
                List<AchievementCardView> achievementProgressCards = achievementService.getAchievementProgress(user.getId()).stream()
                        .map(this::toAchievementCardView)
                        .toList();

                model.addAttribute("todayMissionCards", todayMissionCards);
                model.addAttribute("achievementProgressCards", achievementProgressCards);
            });
        }

        log.info("GET /achievements controller preparation took {} ms", elapsedMs(start));
        return "achievement/ListAchievement";
    }

    // REST API

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @GetMapping("/api/user/{userId}")
    @ResponseBody
    public ResponseEntity<List<UserAchievement>> getAchievementsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(achievementService.getAchievementsByUserId(userId));
    }

    @GetMapping("/api/distribution")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AchievementService.AchievementDistribution>> getAchievementDistribution() {
        return ResponseEntity.ok(achievementService.getAchievementDistribution());
    }

    @PostMapping("/api")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Achievement> createAchievement(@RequestBody @Valid AchievementCreateForm form) {
        Achievement created = achievementService.createAchievement(
                form.getName(),
                form.getMilestone(),
                form.getRequirementType(),
                form.getTargetValue()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/{achievementId}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Achievement> updateAchievement(
            @PathVariable Long achievementId,
            @RequestBody @Valid AchievementCreateForm form
    ) {
        Achievement updated = achievementService.updateAchievement(
                achievementId,
                form.getName(),
                form.getMilestone(),
                form.getRequirementType(),
                form.getTargetValue()
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/{achievementId}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAchievement(@PathVariable Long achievementId) {
        achievementService.deleteAchievement(achievementId);
        return ResponseEntity.ok("Achievement berhasil dihapus");
    }

    @PostMapping("/api/user/toggle-display/{achievementId}")
    @ResponseBody
    public ResponseEntity<String> toggleDisplay(@PathVariable Long achievementId, Authentication authentication) {
        return currentUserResolver.resolveUser(authentication)
                .map(user -> {
                    try {
                        achievementService.toggleDisplayAchievement(user.getId(), achievementId);
                        return ResponseEntity.ok("Status display berhasil diubah");
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(e.getMessage());
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Harap login terlebih dahulu"));
    }

    @PostMapping("/api/daily-mission")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createDailyMission(
            @RequestParam String title,
            @RequestParam int targetCount,
            @RequestParam(defaultValue = "false") boolean primary,
            @RequestParam(required = false) Long categoryId
    ) {
        dailyMissionService.createDailyMission(title, targetCount, primary, categoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Misi Harian berhasil dibuat");
    }

    // EXCEPTION HANDLER
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> handleDuplicate(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    // EDIT MISI HARIAN
    @PutMapping("/api/daily-mission/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateDailyMission(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam int targetCount,
            @RequestParam(defaultValue = "false") boolean primary,
            @RequestParam(required = false) Long categoryId) {
        try {
            dailyMissionService.updateDailyMission(id, title, targetCount, primary, categoryId);
            return ResponseEntity.ok("Misi Harian berhasil diperbarui");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE MISI HARIAN
    @DeleteMapping("/api/daily-mission/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDailyMission(@PathVariable Long id) {
        try {
            dailyMissionService.deleteDailyMission(id);
            return ResponseEntity.ok("Misi Harian berhasil dihapus");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private DailyMissionCardView toDailyMissionCardView(
            DailyMission mission,
            id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress progress,
            Map<Long, String> categoryNamesById
    ) {
        int currentProgress = progress == null ? 0 : progress.getCurrentProgress();
        boolean completed = progress != null && progress.isCompleted();
        int progressPercent = mission.getTargetCount() <= 0
                ? 0
                : Math.min(100, (int) Math.floor(((double) currentProgress / mission.getTargetCount()) * 100));
        String categoryLabel = mission.getCategoryId() == null
                ? "All categories"
                : categoryNamesById.getOrDefault(mission.getCategoryId(), "Unknown category");
        String progressLabel = completed ? "Completed \u2713" : currentProgress + "/" + mission.getTargetCount();
        return new DailyMissionCardView(
                mission.getId(),
                mission.getTitle(),
                mission.getTargetCount(),
                mission.getCategoryId(),
                categoryLabel,
                progressPercent,
                progressLabel,
                completed,
                mission.isPrimary()
        );
    }

    private AchievementCardView toAchievementCardView(AchievementService.AchievementProgress progress) {
        String currentValueText = formatProgressValue(progress.currentValue(), progress.requirementType());
        String progressLabel = progress.unlocked()
                ? "Completed \u2713"
                : currentValueText + "/" + progress.targetValue();
        return new AchievementCardView(
                progress.achievementId(),
                progress.achievementName(),
                progress.milestone(),
                progress.progressPercent(),
                progressLabel,
                progress.unlocked(),
                progress.displayed(),
                progress.requirementType()
        );
    }

    private String formatProgressValue(double value, AchievementRequirementType requirementType) {
        if (requirementType == AchievementRequirementType.TOTAL_SCORE) {
            long roundedValue = Math.round(Math.floor(value));
            return Long.toString(roundedValue);
        }
        return Integer.toString((int) Math.floor(value));
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private record DailyMissionCardView(
            Long id,
            String title,
            int targetCount,
            Long categoryId,
            String categoryLabel,
            int progressPercent,
            String progressLabel,
            boolean completed,
            boolean primary
    ) {
    }

    private record AchievementCardView(
            Long id,
            String name,
            String milestone,
            int progressPercent,
            String progressLabel,
            boolean unlocked,
            boolean displayed,
            AchievementRequirementType requirementType
    ) {
    }
}
