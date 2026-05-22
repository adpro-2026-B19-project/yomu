package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TextServiceImpl implements ITextService {

    private final TextRepository textRepository;
    private final CategoryRepository categoryRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;

    public TextServiceImpl(TextRepository textRepository,
                           CategoryRepository categoryRepository,
                           QuizAttemptRepository quizAttemptRepository,
                           QuestionRepository questionRepository,
                           OptionRepository optionRepository){
        this.textRepository = textRepository;
        this.categoryRepository = categoryRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    public List<Text> getAllTexts(){
        return textRepository.findByPublishedTrue();
    }

    @Override
    public Page<Text> getAllTexts(int page, int size){
        return textRepository.findByPublishedTrue(PageRequest.of(page, size));
    }

    @Override
    public Text getTextById(Long id){
        return textRepository.findById(id).orElseThrow();
    }

    @Override
    public Text getPublishedTextById(Long id) {
        return textRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Text tidak ditemukan"));
    }

    @Override
    public Text createText(String title, String content, Long categoryId, String userId){
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        Text text = new Text(title, content, category, userId);
        return textRepository.save(text);
    }

    @Override
    @Transactional
    public void deleteText(Long textId) {
        quizAttemptRepository.deleteByTextId(textId);
        optionRepository.deleteByTextId(textId);
        questionRepository.deleteByTextId(textId);
        textRepository.deleteById(textId);
    }

    @Override
    @Transactional
    public void publishText(Long textId) {
        Text text = getTextById(textId);
        List<Question> questions = questionRepository.findByTextId(textId);
        
        if (questions.isEmpty()) {
            throw new IllegalStateException("Cannot publish text without any questions.");
        }

        for (Question question : questions) {
            List<Option> options = question.getOptions();
            if (options == null || options.size() < 2) {
                throw new IllegalStateException("Each question must have at least two options.");
            }

            long correctOptionCount = options.stream()
                    .filter(Option::isCorrect)
                    .count();
            if (correctOptionCount != 1) {
                throw new IllegalStateException("Each question must have exactly one correct option.");
            }
        }
        
        text.setPublished(true);
        textRepository.save(text);
    }
    
    @Override
    public List<Text> getAllTextsAdmin(Long categoryId, Boolean published) {
        Specification<Text> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (published != null) {
                predicates.add(cb.equal(root.get("published"), published));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return textRepository.findAll(spec);
    }
}
