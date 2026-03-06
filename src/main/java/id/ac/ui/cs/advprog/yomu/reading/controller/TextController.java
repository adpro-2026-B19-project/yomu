package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.dto.CreateTextRequest;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/texts")
public class TextController {

    private final TextRepository textRepository;
    private final CategoryRepository categoryRepository;

    public TextController(TextRepository textRepository,
                          CategoryRepository categoryRepository) {
        this.textRepository = textRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String getAllTexts(Model model) {
        List<Text> texts = textRepository.findAll();
        model.addAttribute("texts", texts);
        return "reading/texts";
    }

    @GetMapping("/{id}")
    public String getTextDetail(@PathVariable Long id, Model model) {

        Text text = textRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Text tidak ditemukan"));

        model.addAttribute("text", text);

        return "reading/text-detail";
    }

    @GetMapping("/create")
    public String createTextForm(Model model) {

        List<Category> categories = categoryRepository.findAll();

        model.addAttribute("categories", categories);
        model.addAttribute("createTextRequest", new CreateTextRequest());

        return "reading/create-text";
    }

    @PostMapping("/create")
    public String createText(@ModelAttribute CreateTextRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Kategori tidak ditemukan"));

        Text text = new Text();
        text.setTitle(request.getTitle());
        text.setContent(request.getContent());
        text.setCategory(category);

        textRepository.save(text);

        return "redirect:/texts";
    }
}