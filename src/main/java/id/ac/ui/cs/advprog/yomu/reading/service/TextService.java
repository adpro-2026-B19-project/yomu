package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TextService {

    private final TextRepository textRepository;
    private final CategoryRepository categoryRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TextService(TextRepository textRepository,
                       CategoryRepository categoryRepository,
                       QuizAttemptRepository quizAttemptRepository,
                       QuestionRepository questionRepository,
                       OptionRepository optionRepository,
                       ApplicationEventPublisher eventPublisher){
        this.textRepository = textRepository;
        this.categoryRepository = categoryRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Text> getAllTexts(){
        return textRepository.findByPublishedTrue();
    }

    public Text getTextById(Long id){
        return textRepository.findById(id).orElseThrow();
    }

    public Text getPublishedTextById(Long id) {
        return textRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Text tidak ditemukan"));
    }

    public Text createText(String title, String content, Long categoryId, String userId){
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        Text text = new Text(title, content, category, userId);
        return textRepository.save(text);
    }

    public boolean hasUserAttemptedQuiz(String userId, Long textId) {
        return quizAttemptRepository.existsByUserIdAndTextId(userId, textId);
    }

    public QuizAttempt getQuizResult(String userId, Long textId) {
        return quizAttemptRepository.findByUserIdAndTextId(userId, textId).orElse(null);
    }

    public QuizAttempt submitQuiz(Long textId, String userId, Map<String, String> formData) {
        if (hasUserAttemptedQuiz(userId, textId)) {
            throw new IllegalStateException("User has already attempted this quiz.");
        }

        Text text = getPublishedTextById(textId);
        List<Question> questions = questionRepository.findByTextId(textId);
        
        int totalQuestions = questions.size();
        if (totalQuestions == 0) {
            throw new IllegalStateException("No questions available for this text.");
        }

        int correctAnswers = 0;

        for (Question question : questions) {
            String answerOptionIdStr = formData.get("question_" + question.getId());
            if (answerOptionIdStr != null) {
                try {
                    Long optionId = Long.parseLong(answerOptionIdStr);
                    Option selectedOption = optionRepository.findById(optionId).orElse(null);
                    if (selectedOption != null && selectedOption.isCorrect()) {
                        correctAnswers++;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        double accuracy = (double) correctAnswers / totalQuestions;
        double score = accuracy * 100;

        QuizAttempt attempt = new QuizAttempt(text, userId, score, accuracy);
        quizAttemptRepository.save(attempt);

        QuizCompletedEvent event = new QuizCompletedEvent(
                UUID.randomUUID(),
                UUID.fromString(userId),
                mapTextIdToEventId(textId),
                score,
                accuracy,
                LocalDateTime.ofInstant(attempt.getTimestamp(), java.time.ZoneOffset.UTC)
        );
        eventPublisher.publishEvent(event);

        return attempt;
    }
    
    @Transactional
    public void deleteText(Long textId) {
        quizAttemptRepository.deleteByTextId(textId);
        optionRepository.deleteByTextId(textId);
        questionRepository.deleteByTextId(textId);
        textRepository.deleteById(textId);
    }
    
    public id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse getUserReadingStats(String userId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId);
        int totalCompleted = attempts.size();
        
        if (totalCompleted == 0) {
            return new id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse(0, 0.0, 0.0);
        }
        
        double totalAccuracy = 0;
        double totalScore = 0;
        for (QuizAttempt attempt : attempts) {
            totalAccuracy += attempt.getAccuracy();
            totalScore += attempt.getScore();
        }
        
        double averageAccuracy = totalAccuracy / totalCompleted;
        return new id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse(totalCompleted, averageAccuracy, totalScore);
    }

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
    
    public List<Text> getAllTextsAdmin(Long categoryId, Boolean published) {
        List<Text> texts = textRepository.findAll();
        // Since we don't have specification criteria yet, let's just stream filter
        return texts.stream()
                .filter(t -> categoryId == null || t.getCategory().getId().equals(categoryId))
                .filter(t -> published == null || t.isPublished() == published)
                .toList();
    }
    
    public List<QuizAttempt> getUserQuizHistory(String userId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId);
        attempts.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())); // Sort descending
        return attempts;
    }

    private UUID mapTextIdToEventId(Long textId) {
        return UUID.nameUUIDFromBytes(("reading-text:" + textId).getBytes(StandardCharsets.UTF_8));
    }
}
