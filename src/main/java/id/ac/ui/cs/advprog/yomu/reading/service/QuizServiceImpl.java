package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements IQuizService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ITextService textService;

    public QuizServiceImpl(QuizAttemptRepository quizAttemptRepository,
                           QuestionRepository questionRepository,
                           OptionRepository optionRepository,
                           ApplicationEventPublisher eventPublisher,
                           ITextService textService) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.eventPublisher = eventPublisher;
        this.textService = textService;
    }

    @Override
    public boolean hasUserAttemptedQuiz(String userId, Long textId) {
        return quizAttemptRepository.existsByUserIdAndTextId(userId, textId);
    }

    @Override
    public QuizAttempt getQuizResult(String userId, Long textId) {
        return quizAttemptRepository.findByUserIdAndTextId(userId, textId).orElse(null);
    }

    @Override
    public QuizAttempt submitQuiz(Long textId, String userId, Map<String, String> formData) {
        if (hasUserAttemptedQuiz(userId, textId)) {
            throw new IllegalStateException("User has already attempted this quiz.");
        }

        Text text = textService.getPublishedTextById(textId);
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
                textId,
                score,
                accuracy,
                LocalDateTime.ofInstant(attempt.getTimestamp(), java.time.ZoneOffset.UTC)
        );
        eventPublisher.publishEvent(event);

        return attempt;
    }

    @Override
    public UserReadingStatResponse getUserReadingStats(String userId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId);
        int totalCompleted = attempts.size();
        
        if (totalCompleted == 0) {
            return new UserReadingStatResponse(0, 0.0, 0.0);
        }
        
        double totalAccuracy = 0;
        double totalScore = 0;
        for (QuizAttempt attempt : attempts) {
            totalAccuracy += attempt.getAccuracy();
            totalScore += attempt.getScore();
        }
        
        double averageAccuracy = totalAccuracy / totalCompleted;
        return new UserReadingStatResponse(totalCompleted, averageAccuracy, totalScore);
    }

    @Override
    public List<QuizAttempt> getUserQuizHistory(String userId) {
        List<QuizAttempt> attempts = new ArrayList<>(quizAttemptRepository.findByUserId(userId));
        attempts.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())); // Sort descending
        return attempts;
    }

    @Override
    public List<Question> getQuestionsByTextId(Long textId) {
        return questionRepository.findByTextId(textId);
    }

    @Override
    public void addQuestion(Long textId, String questionText, String optionA, String optionB, String optionC, String optionD, String correctOption) {
        Text text = textService.getTextById(textId);
        
        Question question = new Question();
        question.setQuestion(questionText);
        question.setText(text);
        questionRepository.save(question);

        Option optA = new Option(optionA, "A".equals(correctOption));
        Option optB = new Option(optionB, "B".equals(correctOption));
        Option optC = new Option(optionC, "C".equals(correctOption));
        Option optD = new Option(optionD, "D".equals(correctOption));
        
        optA.setQuestion(question);
        optB.setQuestion(question);
        optC.setQuestion(question);
        optD.setQuestion(question);
        
        optionRepository.saveAll(List.of(optA, optB, optC, optD));
    }

    @Override
    public Long editQuestion(Long questionId, String questionText, Long optionAId, Long optionBId, Long optionCId, Long optionDId, String optionA, String optionB, String optionC, String optionD, String correctOption) {
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
        return textId;
    }

    @Override
    public Long deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow();
        Long textId = question.getText().getId();
        questionRepository.deleteById(questionId);
        return textId;
    }

    private void updateOption(Map<Long, Option> optionsById, Long optionId, String text, boolean correct) {
        Option option = optionsById.get(optionId);
        if (option == null) {
            throw new IllegalArgumentException("Option tidak ditemukan");
        }
        option.setText(text);
        option.setCorrect(correct);
    }

    private UUID mapTextIdToEventId(Long textId) {
        return UUID.nameUUIDFromBytes(("reading-text:" + textId).getBytes(StandardCharsets.UTF_8));
    }
}
