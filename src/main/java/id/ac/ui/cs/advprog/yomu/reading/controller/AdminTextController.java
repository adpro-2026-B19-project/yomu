package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.dto.CreateTextRequest;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/texts")
public class AdminTextController {

    private final TextRepository textRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final id.ac.ui.cs.advprog.yomu.reading.service.TextService textService;

    public AdminTextController(
            TextRepository textRepository,
            CategoryRepository categoryRepository,
            QuestionRepository questionRepository,
            OptionRepository optionRepository,
            id.ac.ui.cs.advprog.yomu.reading.service.TextService textService
    ) {
        this.textRepository = textRepository;
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.textService = textService;
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
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow();

        Text text = new Text();
        text.setTitle(request.getTitle());
        text.setContent(request.getContent());
        text.setCategory(category);
        text.setPublished(false); // default

        textRepository.save(text);

        return "redirect:/admin/texts?success=created";
    }

    @PostMapping("/{id}/delete")
    public String deleteText(@PathVariable Long id) {
        textService.deleteText(id);
        return "redirect:/admin/texts?success=deleted";
    }

    @GetMapping("/{id}/questions")
    public String manageQuestions(@PathVariable Long id, Model model) {
        Text text = textRepository.findById(id).orElseThrow();
        List<Question> questions = questionRepository.findByTextId(id);
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
        Text text = textRepository.findById(id).orElseThrow();
        
        Question question = new Question();
        question.setQuestion(questionText);
        question.setText(text);
        questionRepository.save(question);

        Option optA = new Option(optionA, correctOption.equals("A"));
        Option optB = new Option(optionB, correctOption.equals("B"));
        Option optC = new Option(optionC, correctOption.equals("C"));
        Option optD = new Option(optionD, correctOption.equals("D"));
        
        optA.setQuestion(question);
        optB.setQuestion(question);
        optC.setQuestion(question);
        optD.setQuestion(question);
        
        optionRepository.saveAll(List.of(optA, optB, optC, optD));

        return "redirect:/admin/texts/" + id + "/questions";
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
        Question question = questionRepository.findById(questionId).orElseThrow();
        Long textId = question.getText().getId();

        question.setQuestion(questionText);
        questionRepository.save(question);

        Map<Long, Option> optionsById = question.getOptions().stream()
                .collect(Collectors.toMap(Option::getId, Function.identity()));

        updateOption(optionsById, optionAId, optionA, "A".equals(correctOption));
        updateOption(optionsById, optionBId, optionB, "B".equals(correctOption));
        updateOption(optionsById, optionCId, optionC, "C".equals(correctOption));
        updateOption(optionsById, optionDId, optionD, "D".equals(correctOption));

        optionRepository.saveAll(optionsById.values());

        return "redirect:/admin/texts/" + textId + "/questions";
    }

    @PostMapping("/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow();
        Long textId = question.getText().getId();
        questionRepository.deleteById(questionId);
        return "redirect:/admin/texts/" + textId + "/questions";
    }

    private void updateOption(Map<Long, Option> optionsById, Long optionId, String text, boolean correct) {
        Option option = optionsById.get(optionId);
        if (option == null) {
            throw new IllegalArgumentException("Option tidak ditemukan");
        }
        option.setText(text);
        option.setCorrect(correct);
    }
}
