package id.ac.ui.cs.advprog.yomu.achievement.controller;

import id.ac.ui.cs.advprog.yomu.achievement.service.DailyMissionService;
import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import id.ac.ui.cs.advprog.yomu.achievement.dto.AchievementCreateForm;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final DailyMissionService dailyMissionService;
    private final CurrentUserResolver currentUserResolver;

    // Thymeleaf Page
    @GetMapping
    public String achievementListPage(Model model) {
        List<Achievement> achievements = achievementService.getAllAchievements();
        model.addAttribute("achievements", achievements);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            model.addAttribute("loggedInName", auth.getName());
            currentUserResolver.resolveUser(auth).ifPresent(user -> {
                model.addAttribute("todayMissions", dailyMissionService.getTodayMissions());
                model.addAttribute("userProgress", dailyMissionService.getUserProgress(user.getId()));
            });
        }
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

    @PostMapping("/api")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Achievement> createAchievement(@RequestBody @Valid AchievementCreateForm form) {
        Achievement created = achievementService.createAchievement(form.getName(), form.getMilestone());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateAchievement(@PathVariable Long id, @RequestBody AchievementCreateForm form) {
        try {
            Achievement updated = achievementService.updateAchievement(id, form.getName(), form.getMilestone());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update achievement id={}", id, e);
            return ResponseEntity.badRequest().body("Input tidak valid");
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteAchievement(@PathVariable Long id) {
        try {
            achievementService.deleteAchievement(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete achievement id={}", id, e);
            return ResponseEntity.badRequest().body("Permintaan tidak valid");
        }
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
                        log.warn("Failed to toggle display for userId={} achievementId={}", user.getId(), achievementId, e);
                        return ResponseEntity.badRequest().body("Permintaan tidak valid");
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Harap login terlebih dahulu"));
    }

    @PostMapping("/api/daily-mission")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> createDailyMission(@RequestParam String title, @RequestParam int targetCount) {
        dailyMissionService.createDailyMission(title, targetCount);
        return ResponseEntity.status(HttpStatus.CREATED).body("Misi Harian berhasil dibuat");
    }

    // EXCEPTION HANDLER
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> handleDuplicate(IllegalArgumentException ex) {
        log.warn("Illegal argument in achievement controller", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Permintaan tidak dapat diproses");
    }

    // EDIT MISI HARIAN
    @PutMapping("/api/daily-mission/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> updateDailyMission(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam int targetCount) {
        try {
            dailyMissionService.updateDailyMission(id, title, targetCount);
            return ResponseEntity.ok("Misi Harian berhasil diperbarui");
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update daily mission id={}", id, e);
            return ResponseEntity.badRequest().body("Input tidak valid.");
        }
    }

    // DELETE MISI HARIAN
    @DeleteMapping("/api/daily-mission/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteDailyMission(@PathVariable Long id) {
        try {
            dailyMissionService.deleteDailyMission(id);
            return ResponseEntity.ok("Misi Harian berhasil dihapus");
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete daily mission id={}", id, e);
            return ResponseEntity.badRequest().body("Permintaan tidak valid.");
        }
    }
}