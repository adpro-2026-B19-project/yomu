package id.ac.ui.cs.advprog.yomu.reading.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class QuizServiceImplTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ITextService textService;

    @InjectMocks
    private QuizServiceImpl quizService;

    private Text text;

    @BeforeEach
    void setUp() {
        Category category = new Category("Teknologi");
        text = new Text("Judul Test", "Konten Test", category, "user-123");
        text.setPublished(true);
        try {
            java.lang.reflect.Field idField = Text.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(text, 1L);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Test
    void testHasUserAttemptedQuizTrue() {
        when(quizAttemptRepository.existsByUserIdAndTextId("user1", 1L)).thenReturn(true);
        assertTrue(quizService.hasUserAttemptedQuiz("user1", 1L));
    }

    @Test
    void testSubmitQuizSuccess() {
        Question q1 = new Question();
        Option opt1 = new Option("Benar", true);
        opt1.setQuestion(q1);
        q1.setOptions(List.of(opt1));

        try {
            java.lang.reflect.Field idField = Question.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(q1, 10L);
            idField = Option.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(opt1, 100L);
        } catch (ReflectiveOperationException ignored) {
        }

        String userId = UUID.randomUUID().toString();

        when(textService.getPublishedTextById(1L)).thenReturn(text);
        when(questionRepository.findByTextId(1L)).thenReturn(List.of(q1));
        when(optionRepository.findById(100L)).thenReturn(Optional.of(opt1));
        when(quizAttemptRepository.save(any(id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        java.util.Map<String, String> formData = java.util.Map.of("question_10", "100");

        id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt attempt = quizService.submitQuiz(1L, userId, formData);

        assertNotNull(attempt);
        assertEquals(100.0, attempt.getScore());
        assertEquals(1.0, attempt.getAccuracy());
        verify(quizAttemptRepository, times(1)).save(any(id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt.class));
        verify(eventPublisher, times(1))
                .publishEvent(any(id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent.class));
    }

    @Test
    void testSubmitQuizAlreadyAttempted() {
        when(quizAttemptRepository.existsByUserIdAndTextId("user-1", 1L)).thenReturn(true);

        java.util.Map<String, String> formData = java.util.Map.of("question_10", "100");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                quizService.submitQuiz(1L, "user-1", formData));

        assertEquals("User has already attempted this quiz.", exception.getMessage());
    }
    
    @Test
    void testSubmitQuizWithNoQuestions() {
        when(textService.getPublishedTextById(1L)).thenReturn(text);
        when(questionRepository.findByTextId(1L)).thenReturn(List.of());

        java.util.Map<String, String> formData = java.util.Map.of();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                quizService.submitQuiz(1L, "user-1", formData));

        assertEquals("No questions available for this text.", exception.getMessage());
    }

    @Test
    void testGetUserReadingStatsEmpty() {
        when(quizAttemptRepository.findByUserId("user-1")).thenReturn(List.of());
        UserReadingStatResponse stats = quizService.getUserReadingStats("user-1");
        
        assertEquals(0, stats.getTotalTextsCompleted());
        assertEquals(0.0, stats.getAverageAccuracy());
        assertEquals(0.0, stats.getTotalAccumulatedScore());
    }
    
    @Test
    void testGetUserReadingStatsNonEmpty() {
        QuizAttempt attempt1 = new QuizAttempt(text, "user-1", 100.0, 1.0);
        QuizAttempt attempt2 = new QuizAttempt(text, "user-1", 50.0, 0.5);
        when(quizAttemptRepository.findByUserId("user-1")).thenReturn(List.of(attempt1, attempt2));
        
        UserReadingStatResponse stats = quizService.getUserReadingStats("user-1");
        
        assertEquals(2, stats.getTotalTextsCompleted());
        assertEquals(0.75, stats.getAverageAccuracy());
        assertEquals(150.0, stats.getTotalAccumulatedScore());
    }
    
    @Test
    void testGetUserQuizHistory() {
        QuizAttempt attempt1 = new QuizAttempt(text, "user-1", 100.0, 1.0);
        when(quizAttemptRepository.findByUserId("user-1")).thenReturn(List.of(attempt1));
        
        List<QuizAttempt> history = quizService.getUserQuizHistory("user-1");
        
        assertEquals(1, history.size());
    }
    
    @Test
    void testGetQuizResultFound() {
        QuizAttempt attempt = new QuizAttempt(text, "user-1", 100.0, 1.0);
        when(quizAttemptRepository.findByUserIdAndTextId("user-1", 1L)).thenReturn(Optional.of(attempt));
        
        QuizAttempt result = quizService.getQuizResult("user-1", 1L);
        assertNotNull(result);
    }
    
    @Test
    void testGetQuizResultNotFound() {
        when(quizAttemptRepository.findByUserIdAndTextId("user-1", 1L)).thenReturn(Optional.empty());
        
        QuizAttempt result = quizService.getQuizResult("user-1", 1L);
        assertTrue(result == null);
    }
}
