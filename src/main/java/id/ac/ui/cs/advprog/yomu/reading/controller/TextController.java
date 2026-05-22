package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.TextService;

import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/texts")
public class TextController {

    private static final Logger log = LoggerFactory.getLogger(TextController.class);

    private final QuestionRepository questionRepository;
    private final TextService textService;
    private final CurrentUserResolver currentUserResolver;
    private final AchievementService achievementService;

    public TextController(QuestionRepository questionRepository,
            TextService textService,
            CurrentUserResolver currentUserResolver,
            AchievementService achievementService) {
        this.questionRepository = questionRepository;
        this.textService = textService;
        this.currentUserResolver = currentUserResolver;
        this.achievementService = achievementService;
    }

    @GetMapping
    public String getAllTexts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model) {
        long start = System.nanoTime();
        org.springframework.data.domain.Page<Text> textPage = textService.getAllTexts(page, size);
        model.addAttribute("texts", textPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", textPage.getTotalPages());
        model.addAttribute("hasNext", textPage.hasNext());
        model.addAttribute("hasPrevious", textPage.hasPrevious());
        log.info("GET /texts controller preparation took {} ms", elapsedMs(start));
        return "reading/texts";
    }

    @GetMapping("/history")
    public String getHistory(Model model, Authentication authentication) {
        long start = System.nanoTime();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        AuthUser authUser = currentUserResolver.resolveUser(authentication).orElseThrow();
        List<QuizAttempt> history = textService.getUserQuizHistory(authUser.getId().toString());
        model.addAttribute("history", history);
        log.info("GET /texts/history controller preparation took {} ms", elapsedMs(start));
        return "reading/history";
    }

    @GetMapping("/{id:\\d+}")
    public String getTextDetail(@PathVariable Long id, Model model, Authentication authentication) {
        long start = System.nanoTime();
        try {
            Text text = textService.getPublishedTextById(id);

            boolean hasAttempted = false;
            if (authentication != null && authentication.isAuthenticated()) {
                AuthUser authUser = currentUserResolver.resolveUser(authentication).orElse(null);
                if (authUser != null) {
                    hasAttempted = textService.hasUserAttemptedQuiz(authUser.getId().toString(), id);
                }
            }

            model.addAttribute("text", text);
            model.addAttribute("hasAttempted", hasAttempted);

            log.info("GET /texts/{id} controller preparation took {} ms", elapsedMs(start));
            return "reading/text-detail";
        } catch (ResponseStatusException e) {
            return "redirect:/texts?error=" + e.getReason();
        } catch (Exception e) {
            return "redirect:/texts?error=Terjadi kesalahan saat memuat detail bacaan.";
        }
    }

    @GetMapping("/{id:\\d+}/quiz")
    public String startQuiz(@PathVariable Long id, Model model, Authentication authentication) {
        long start = System.nanoTime();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        AuthUser authUser = currentUserResolver.resolveUser(authentication).orElseThrow();

        try {
            if (textService.hasUserAttemptedQuiz(authUser.getId().toString(), id)) {
                return "redirect:/texts/" + id + "?error=already_attempted";
            }

            Text text = textService.getPublishedTextById(id);

            List<Question> questions = questionRepository.findByTextId(id);

            model.addAttribute("text", text);
            model.addAttribute("questions", questions);

            log.info("GET /texts/{id}/quiz controller preparation took {} ms", elapsedMs(start));
            return "reading/quiz";
        } catch (ResponseStatusException e) {
            return "redirect:/texts?error=" + e.getReason();
        } catch (Exception e) {
            return "redirect:/texts?error=Terjadi kesalahan saat memuat kuis.";
        }
    }

    @PostMapping("/{id:\\d+}/quiz/submit")
    public String submitQuiz(@PathVariable Long id,
            @RequestParam Map<String, String> formData,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        AuthUser authUser = currentUserResolver.resolveUser(authentication).orElseThrow();

        try {
            List<UserAchievement> achievementsBeforeQuiz = achievementService.getAchievementsByUserId(authUser.getId());
            QuizAttempt attempt = textService.submitQuiz(id, authUser.getId().toString(), formData);
            List<UserAchievement> unlockedAchievements = achievementService.getAchievementsByUserId(authUser.getId());
            Set<Long> unlockedBeforeIds = achievementsBeforeQuiz.stream()
                    .map(userAchievement -> userAchievement.getAchievement().getId())
                    .collect(Collectors.toSet());
            List<UnlockedAchievementView> newlyUnlockedAchievements = unlockedAchievements.stream()
                    .filter(userAchievement -> !unlockedBeforeIds.contains(userAchievement.getAchievement().getId()))
                    .map(userAchievement -> new UnlockedAchievementView(
                            userAchievement.getAchievement().getId(),
                            userAchievement.getAchievement().getName(),
                            userAchievement.getAchievement().getMilestone()
                    ))
                    .toList();
            model.addAttribute("attempt", attempt);
            model.addAttribute("textId", id);
            model.addAttribute("newlyUnlockedAchievements", newlyUnlockedAchievements);
            model.addAttribute(
                    "newlyUnlockedAchievementNames",
                    newlyUnlockedAchievements.stream()
                            .map(UnlockedAchievementView::name)
                            .collect(Collectors.joining(", "))
            );
            return "reading/quiz-result";
        } catch (ResponseStatusException e) {
            return "redirect:/texts?error=Teks bacaan telah dihapus oleh admin.";
        } catch (IllegalStateException e) {
            return "redirect:/texts/" + id + "?error=" + e.getMessage();
        } catch (Exception e) {
            return "redirect:/texts?error=Terjadi kesalahan saat mengumpulkan kuis.";
        }
    }


    private record UnlockedAchievementView(
            Long id,
            String name,
            String milestone
    ) {
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

}
