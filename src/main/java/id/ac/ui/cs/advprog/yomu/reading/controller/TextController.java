package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.TextService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/texts")
public class TextController {

    private final QuestionRepository questionRepository;
    private final TextService textService;
    private final CurrentUserResolver currentUserResolver;

    public TextController(QuestionRepository questionRepository,
            TextService textService,
            CurrentUserResolver currentUserResolver) {
        this.questionRepository = questionRepository;
        this.textService = textService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public String getAllTexts(Model model) {
        List<Text> texts = textService.getAllTexts();
        model.addAttribute("texts", texts);
        return "reading/texts";
    }

    @GetMapping("/history")
    public String getHistory(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        AuthUser authUser = currentUserResolver.resolveUser(authentication).orElseThrow();
        List<QuizAttempt> history = textService.getUserQuizHistory(authUser.getId().toString());
        model.addAttribute("history", history);
        return "reading/history";
    }

    @GetMapping("/{id:\\d+}")
    public String getTextDetail(@PathVariable Long id, Model model, Authentication authentication) {
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

        return "reading/text-detail";
    }

    @GetMapping("/{id:\\d+}/quiz")
    public String startQuiz(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        AuthUser authUser = currentUserResolver.resolveUser(authentication).orElseThrow();

        if (textService.hasUserAttemptedQuiz(authUser.getId().toString(), id)) {
            return "redirect:/texts/" + id + "?error=already_attempted";
        }

        Text text = textService.getPublishedTextById(id);

        List<Question> questions = questionRepository.findByTextId(id);

        model.addAttribute("text", text);
        model.addAttribute("questions", questions);

        return "reading/quiz";
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
            QuizAttempt attempt = textService.submitQuiz(id, authUser.getId().toString(), formData);
            model.addAttribute("attempt", attempt);
            model.addAttribute("textId", id);
            return "reading/quiz-result";
        } catch (IllegalStateException e) {
            return "redirect:/texts/" + id + "?error=" + e.getMessage();
        }
    }

}
