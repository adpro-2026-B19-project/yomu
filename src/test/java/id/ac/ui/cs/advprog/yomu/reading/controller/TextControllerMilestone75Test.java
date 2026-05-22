package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.ITextService;
import id.ac.ui.cs.advprog.yomu.reading.service.IQuizService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextControllerMilestone75Test {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ITextService textService;

    @Mock
    private IQuizService quizService;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Mock
    private AchievementService achievementService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TextController textController;

    @Test
    void getHistoryShouldRedirectAnonymousUserToLogin() {
        String view = textController.getHistory(new ExtendedModelMap(), null);

        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void getHistoryShouldRedirectUnauthenticatedUserToLogin() {
        when(authentication.isAuthenticated()).thenReturn(false);

        String view = textController.getHistory(new ExtendedModelMap(), authentication);

        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void getHistoryShouldRenderUserQuizHistory() {
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        Text text = new Text();
        text.setTitle("Mengenali Berita Palsu");
        QuizAttempt attempt = new QuizAttempt(text, authUser.getId().toString(), 80.0, 0.8);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(quizService.getUserQuizHistory(authUser.getId().toString())).thenReturn(List.of(attempt));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getHistory(model, authentication);

        assertThat(view).isEqualTo("reading/history");
        assertThat(model.getAttribute("history")).isEqualTo(List.of(attempt));
    }

    @Test
    void submitQuizShouldRedirectAnonymousUserToLogin() {
        String view = textController.submitQuiz(
                1L,
                Map.of("question_1", "2"),
                new ExtendedModelMap(),
                null
        );

        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void submitQuizShouldRedirectUnauthenticatedUserToLogin() {
        when(authentication.isAuthenticated()).thenReturn(false);

        String view = textController.submitQuiz(
                1L,
                Map.of("question_1", "2"),
                new ExtendedModelMap(),
                authentication
        );

        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void submitQuizShouldRenderQuizResultWhenSuccessful() {
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        Text text = new Text();
        text.setTitle("Sample Reading");
        QuizAttempt attempt = new QuizAttempt(text, authUser.getId().toString(), 100.0, 1.0);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(achievementService.getAchievementsByUserId(authUser.getId())).thenReturn(List.of(), List.of());
        when(quizService.submitQuiz(5L, authUser.getId().toString(), Map.of("question_1", "2")))
                .thenReturn(attempt);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.submitQuiz(
                5L,
                Map.of("question_1", "2"),
                model,
                authentication
        );

        assertThat(view).isEqualTo("reading/quiz-result");
        assertThat(model.getAttribute("attempt")).isEqualTo(attempt);
        assertThat(model.getAttribute("textId")).isEqualTo(5L);
    }

    @Test
    void submitQuizShouldRedirectToTextDetailWhenAlreadyAttempted() {
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(achievementService.getAchievementsByUserId(authUser.getId())).thenReturn(List.of());
        when(quizService.submitQuiz(5L, authUser.getId().toString(), Map.of("question_1", "2")))
                .thenThrow(new IllegalStateException("User has already attempted this quiz."));

        String view = textController.submitQuiz(
                5L,
                Map.of("question_1", "2"),
                new ExtendedModelMap(),
                authentication
        );

        assertThat(view).isEqualTo("redirect:/texts/5?error=User has already attempted this quiz.");
    }

    private void setUserId(AuthUser authUser) {
        try {
            Field idField = AuthUser.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(authUser, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
