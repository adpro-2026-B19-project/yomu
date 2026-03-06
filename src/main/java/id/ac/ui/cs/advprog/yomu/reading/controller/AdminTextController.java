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

import java.util.List;

@Controller
@RequestMapping("/admin/texts")
public class AdminTextController {

    private final TextRepository textRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;

    public AdminTextController(
            TextRepository textRepository,
            CategoryRepository categoryRepository,
            QuestionRepository questionRepository,
            OptionRepository optionRepository
    ) {
        this.textRepository = textRepository;
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @GetMapping("/create")
    public String createTextPage(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/create-text";
    }

    @PostMapping
    public String createText(@ModelAttribute CreateTextRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow();

        Text text = new Text();
        text.setTitle(request.getTitle());
        text.setContent(request.getContent());
        text.setCategory(category);

        textRepository.save(text);

        Question question = new Question();
        question.setQuestion(request.getQuestion());
        question.setText(text);

        questionRepository.save(question);

        Option optionA = new Option();
        optionA.setText(request.getOptionA());
        optionA.setCorrect(request.getCorrect().equals("A"));
        optionA.setQuestion(question);

        Option optionB = new Option();
        optionB.setText(request.getOptionB());
        optionB.setCorrect(request.getCorrect().equals("B"));
        optionB.setQuestion(question);

        Option optionC = new Option();
        optionC.setText(request.getOptionC());
        optionC.setCorrect(request.getCorrect().equals("C"));
        optionC.setQuestion(question);

        Option optionD = new Option();
        optionD.setText(request.getOptionD());
        optionD.setCorrect(request.getCorrect().equals("D"));
        optionD.setQuestion(question);

        optionRepository.saveAll(List.of(optionA, optionB, optionC, optionD));

        return "redirect:/admin/texts/create?success";
    }
}