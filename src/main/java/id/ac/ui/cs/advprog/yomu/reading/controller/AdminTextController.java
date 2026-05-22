package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.dto.CreateTextRequest;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.ITextService;
import id.ac.ui.cs.advprog.yomu.reading.service.IQuizService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/texts")
public class AdminTextController {

    private final CategoryRepository categoryRepository;
    private final ITextService textService;
    private final IQuizService quizService;

    public AdminTextController(
            CategoryRepository categoryRepository,
            ITextService textService,
            IQuizService quizService
    ) {
        this.categoryRepository = categoryRepository;
        this.textService = textService;
        this.quizService = quizService;
    }

    @GetMapping("/create")
    public String createTextPage(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "reading/create-text";
    }

    @GetMapping
    public String adminDashboard(@RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Boolean published,
                                 Model model) {
        List<Text> texts = textService.getAllTextsAdmin(categoryId, published);
        model.addAttribute("texts", texts);
        model.addAttribute("categories", categoryRepository.findAll());
        return "reading/admin-dashboard";
    }

    @PostMapping("/{id}/publish")
    public String publishText(@PathVariable Long id) {
        try {
            textService.publishText(id);
            return "redirect:/admin/texts?success=published";
        } catch (IllegalStateException e) {
            return "redirect:/admin/texts?error=" + e.getMessage();
        }
    }

    @PostMapping
    public String createText(@ModelAttribute CreateTextRequest request) {
        // Assume user ID is known or we pass a system user, since the original didn't use real user ID
        // The original did: text.setCreatedByUserId(null) effectively (not set)
        textService.createText(request.getTitle(), request.getContent(), request.getCategoryId(), null);
        return "redirect:/admin/texts?success=created";
    }

    @PostMapping("/{id}/delete")
    public String deleteText(@PathVariable Long id) {
        textService.deleteText(id);
        return "redirect:/admin/texts?success=deleted";
    }

    @GetMapping("/{id}/questions")
    public String manageQuestions(@PathVariable Long id, Model model) {
        Text text = textService.getTextById(id);
        List<Question> questions = quizService.getQuestionsByTextId(id);
        questions.forEach(question -> {
            if (question.getOptions() != null) {
                question.setOptions(question.getOptions().stream()
                        .sorted(Comparator.comparing(Option::getId))
                        .toList());
            }
        });
        model.addAttribute("text", text);
        model.addAttribute("questions", questions);
        return "reading/admin-manage-questions";
    }

    @PostMapping("/{id}/questions")
    public String addQuestion(@PathVariable Long id, @RequestParam String questionText,
                              @RequestParam String optionA, @RequestParam String optionB,
                              @RequestParam String optionC, @RequestParam String optionD,
                              @RequestParam String correctOption) {
        quizService.addQuestion(id, questionText, optionA, optionB, optionC, optionD, correctOption);
        return "redirect:/admin/texts/" + id + "/questions";
    }

    @PostMapping(value = "/{id}/questions", params = "async")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> addQuestionAsync(
            @PathVariable Long id,
            @RequestParam String questionText,
            @RequestParam String optionA, @RequestParam String optionB,
            @RequestParam String optionC, @RequestParam String optionD,
            @RequestParam String correctOption) {
        try {
            quizService.addQuestion(id, questionText, optionA, optionB, optionC, optionD, correctOption);
            List<Question> questions = quizService.getQuestionsByTextId(id);
            Question newest = questions.get(questions.size() - 1);
            List<Option> opts = newest.getOptions().stream()
                    .sorted(Comparator.comparing(Option::getId))
                    .toList();
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("id", newest.getId());
            body.put("question", newest.getQuestion());
            body.put("options", opts.stream().map(o -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", o.getId());
                m.put("text", o.getText());
                m.put("correct", o.isCorrect());
                return m;
            }).toList());
            return org.springframework.http.ResponseEntity.ok(body);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/questions/{questionId}/edit")
    public String editQuestion(@PathVariable Long questionId,
                               @RequestParam String questionText,
                               @RequestParam Long optionAId,
                               @RequestParam Long optionBId,
                               @RequestParam Long optionCId,
                               @RequestParam Long optionDId,
                               @RequestParam String optionA,
                               @RequestParam String optionB,
                               @RequestParam String optionC,
                               @RequestParam String optionD,
                               @RequestParam String correctOption) {
        
        Long textId = quizService.editQuestion(questionId, questionText, optionAId, optionBId, optionCId, optionDId, optionA, optionB, optionC, optionD, correctOption);
        return "redirect:/admin/texts/" + textId + "/questions"; 
    }

    @PostMapping("/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long questionId) {
        Long textId = quizService.deleteQuestion(questionId);
        return "redirect:/admin/texts/" + textId + "/questions";
    }
}
