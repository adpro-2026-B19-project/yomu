package id.ac.ui.cs.advprog.yomu.reading;

import id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse;
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
import id.ac.ui.cs.advprog.yomu.reading.service.QuizServiceImpl;
import id.ac.ui.cs.advprog.yomu.reading.service.TextServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextServiceMilestone75Test {

    @Mock
    private TextRepository textRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TextServiceImpl textService;

    private QuizServiceImpl quizService;

    private Category category;
    private Text text;

    @BeforeEach
    void setUp() {
        quizService = new QuizServiceImpl(quizAttemptRepository, questionRepository, optionRepository, eventPublisher, textService);

        category = new Category("Digital Literacy");
        setId(category, 1L);

        text = new Text();
        text.setTitle("Mengenali Berita Palsu");
        text.setContent("Konten literasi digital");
        text.setCategory(category);
        text.setPublished(false);
        setId(text, 10L);
    }

    @Test
    void getUserReadingStatsShouldReturnZeroWhenUserHasNoAttempt() {
        when(quizAttemptRepository.findByUserId("user-1")).thenReturn(List.of());

        UserReadingStatResponse result = quizService.getUserReadingStats("user-1");

        assertThat(result.getTotalTextsCompleted()).isZero();
        assertThat(result.getAverageAccuracy()).isZero();
        assertThat(result.getTotalAccumulatedScore()).isZero();
    }

    @Test
    void getUserReadingStatsShouldCalculateCompletedTextsAverageAccuracyAndTotalScore() {
        QuizAttempt attempt1 = new QuizAttempt(text, "user-1", 100.0, 1.0);
        QuizAttempt attempt2 = new QuizAttempt(text, "user-1", 50.0, 0.5);

        when(quizAttemptRepository.findByUserId("user-1")).thenReturn(List.of(attempt1, attempt2));

        UserReadingStatResponse result = quizService.getUserReadingStats("user-1");

        assertThat(result.getTotalTextsCompleted()).isEqualTo(2);
        assertThat(result.getAverageAccuracy()).isEqualTo(0.75);
        assertThat(result.getTotalAccumulatedScore()).isEqualTo(150.0);
    }

    @Test
    void getAllTextsAdminShouldFilterByCategoryAndPublishedStatus() {
        Category otherCategory = new Category("Science");
        setId(otherCategory, 2L);

        Text publishedTarget = new Text();
        publishedTarget.setTitle("Target");
        publishedTarget.setCategory(category);
        publishedTarget.setPublished(true);

        Text draftTarget = new Text();
        draftTarget.setTitle("Draft");
        draftTarget.setCategory(category);
        draftTarget.setPublished(false);

        Text otherPublished = new Text();
        otherPublished.setTitle("Other");
        otherPublished.setCategory(otherCategory);
        otherPublished.setPublished(true);

        when(textRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(publishedTarget));

        List<Text> result = textService.getAllTextsAdmin(1L, true);

        assertThat(result).containsExactly(publishedTarget);
    }

    @Test
    void publishTextShouldRejectTextWithoutQuestions() {
        when(textRepository.findById(10L)).thenReturn(Optional.of(text));
        when(questionRepository.findByTextId(10L)).thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> textService.publishText(10L)
        );

        assertThat(exception.getMessage()).isEqualTo("Cannot publish text without any questions.");
    }

    @Test
    void publishTextShouldRejectQuestionWithoutExactlyOneCorrectOption() {
        Question question = new Question();
        question.setText(text);
        question.setOptions(List.of(
                new Option("A", true),
                new Option("B", true),
                new Option("C", false),
                new Option("D", false)
        ));

        when(textRepository.findById(10L)).thenReturn(Optional.of(text));
        when(questionRepository.findByTextId(10L)).thenReturn(List.of(question));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> textService.publishText(10L)
        );

        assertThat(exception.getMessage()).isEqualTo("Each question must have exactly one correct option.");
    }

    @Test
    void publishTextShouldSetPublishedTrueWhenContentIsValid() {
        Question question = new Question();
        question.setText(text);
        question.setOptions(List.of(
                new Option("A", true),
                new Option("B", false),
                new Option("C", false),
                new Option("D", false)
        ));

        when(textRepository.findById(10L)).thenReturn(Optional.of(text));
        when(questionRepository.findByTextId(10L)).thenReturn(List.of(question));

        textService.publishText(10L);

        assertThat(text.isPublished()).isTrue();
        verify(textRepository).save(text);
    }

    @Test
    void getUserQuizHistoryShouldReturnAttemptsSortedDescendingByTimestamp() {
        QuizAttempt older = new QuizAttempt(text, "user-1", 50.0, 0.5);
        older.setTimestamp(Instant.parse("2026-05-01T10:00:00Z"));

        QuizAttempt newer = new QuizAttempt(text, "user-1", 100.0, 1.0);
        newer.setTimestamp(Instant.parse("2026-05-07T10:00:00Z"));

        when(quizAttemptRepository.findByUserId("user-1")).thenReturn(List.of(older, newer));

        List<QuizAttempt> result = quizService.getUserQuizHistory("user-1");

        assertThat(result).containsExactly(newer, older);
    }

    @Test
    void submitQuizShouldSaveZeroScoreWhenUserDoesNotAnswerAnyQuestion() {
        Question question = new Question();
        setId(question, 99L);
        question.setText(text);

        when(quizAttemptRepository.existsByUserIdAndTextId("00000000-0000-0000-0000-000000000001", 10L))
                .thenReturn(false);
        when(textRepository.findByIdAndPublishedTrue(10L)).thenReturn(Optional.of(text));
        when(questionRepository.findByTextId(10L)).thenReturn(List.of(question));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizAttempt result = quizService.submitQuiz(
                10L,
                "00000000-0000-0000-0000-000000000001",
                java.util.Map.of()
        );

        assertThat(result.getScore()).isEqualTo(0.0);
        assertThat(result.getAccuracy()).isEqualTo(0.0);
    }

    private void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}