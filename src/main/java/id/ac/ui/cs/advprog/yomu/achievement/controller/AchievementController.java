package id.ac.ui.cs.advprog.yomu.achievement.controller;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    // ─── Thymeleaf Page ────────────────────────────────────────────────────────

    @GetMapping
    public String achievementListPage(Model model) {
        List<Achievement> achievements = achievementService.getAllAchievements();
        model.addAttribute("achievements", achievements);
        return "achievement/ListAchievement";
    }

    // ─── REST API ──────────────────────────────────────────────────────────────

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Achievement> createAchievement(@RequestParam String name,
                                                         @RequestParam String milestone) {
        Achievement created = achievementService.createAchievement(name, milestone);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}