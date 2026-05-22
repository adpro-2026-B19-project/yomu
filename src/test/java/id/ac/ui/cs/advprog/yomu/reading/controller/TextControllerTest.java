package id.ac.ui.cs.advprog.yomu.reading.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.ITextService;
import id.ac.ui.cs.advprog.yomu.reading.service.IQuizService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TextControllerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ITextService textService;

    @Mock
    private IQuizService quizService;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TextController textController;

    @Mock
    private AchievementService achievementService;

    @Test
    @SuppressWarnings("unchecked")
    void getAllTextsShouldRenderListPage() {
        Text t = new Text();
        t.setTitle("A");
        org.springframework.data.domain.Page<Text> pagedTexts = org.mockito.Mockito.mock(org.springframework.data.domain.Page.class);
        when(pagedTexts.getContent()).thenReturn(List.of(t));
        when(pagedTexts.getTotalPages()).thenReturn(1);
        when(pagedTexts.hasNext()).thenReturn(false);
        when(pagedTexts.hasPrevious()).thenReturn(false);
        when(textService.getAllTexts(0, 6)).thenReturn(pagedTexts);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getAllTexts(0, 6, model, null);

        assertThat(view).isEqualTo("reading/texts");
        assertThat(model.getAttribute("texts")).isEqualTo(List.of(t));
        assertThat(model.getAttribute("currentPage")).isEqualTo(0);
        assertThat(model.getAttribute("totalPages")).isEqualTo(1);
        assertThat(model.getAttribute("hasNext")).isEqualTo(false);
        assertThat(model.getAttribute("hasPrevious")).isEqualTo(false);
    }

    @Test
    void getTextDetailShouldRedirectWhenTextNotFoundOrDeleted() {
        when(textService.getPublishedTextById(99L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Text tidak ditemukan"));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getTextDetail(99L, model, null);

        assertThat(view).isEqualTo("redirect:/texts?error=Text tidak ditemukan");
    }

    @Test
    void getTextDetailShouldRenderWhenPublishedTextFound() {
        Text t = new Text();
        t.setTitle("Hello");
        when(textService.getPublishedTextById(1L)).thenReturn(t);
        when(authentication.isAuthenticated()).thenReturn(false);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getTextDetail(1L, model, authentication);

        assertThat(view).isEqualTo("reading/text-detail");
        assertThat(model.getAttribute("text")).isEqualTo(t);
        assertThat(model.getAttribute("hasAttempted")).isEqualTo(false);
    }

    @Test
    void getTextDetailShouldMarkAttemptedWhenAuthenticatedUserAlreadyAttempted() {
        Text text = new Text();
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        when(textService.getPublishedTextById(1L)).thenReturn(text);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(quizService.hasUserAttemptedQuiz(authUser.getId().toString(), 1L)).thenReturn(true);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getTextDetail(1L, model, authentication);

        assertThat(view).isEqualTo("reading/text-detail");
        assertThat(model.getAttribute("hasAttempted")).isEqualTo(true);
    }

    @Test
    void getTextDetailShouldNotMarkAttemptedWhenResolverReturnsEmpty() {
        Text text = new Text();

        when(textService.getPublishedTextById(1L)).thenReturn(text);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.empty());

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getTextDetail(1L, model, authentication);

        assertThat(view).isEqualTo("reading/text-detail");
        assertThat(model.getAttribute("hasAttempted")).isEqualTo(false);
    }

    @Test
    void startQuizShouldRedirectAnonymousUserToLogin() {
        String view = textController.startQuiz(1L, new ExtendedModelMap(), null);
        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void startQuizShouldRedirectUnauthenticatedUserToLogin() {
        when(authentication.isAuthenticated()).thenReturn(false);

        String view = textController.startQuiz(1L, new ExtendedModelMap(), authentication);

        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void startQuizShouldRedirectWhenUserAlreadyAttempted() {
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(quizService.hasUserAttemptedQuiz(authUser.getId().toString(), 5L)).thenReturn(true);

        String view = textController.startQuiz(5L, new ExtendedModelMap(), authentication);

        assertThat(view).isEqualTo("redirect:/texts/5?error=already_attempted");
    }

    @Test
    void startQuizShouldRenderQuizForPublishedText() {
        Text text = new Text();
        Question question = new Question();
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(quizService.hasUserAttemptedQuiz(authUser.getId().toString(), 5L)).thenReturn(false);
        when(textService.getPublishedTextById(5L)).thenReturn(text);
        when(quizService.getQuestionsByTextId(anyLong())).thenReturn(List.of(question));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.startQuiz(5L, model, authentication);

        assertThat(view).isEqualTo("reading/quiz");
        assertThat(model.getAttribute("text")).isEqualTo(text);
        assertThat(model.getAttribute("questions")).isEqualTo(List.of(question));
    }

    @Test
    void startQuizShouldRedirectWhenTextNotFoundOrDeleted() {
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(quizService.hasUserAttemptedQuiz(authUser.getId().toString(), 99L)).thenReturn(false);
        when(textService.getPublishedTextById(99L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Text tidak ditemukan"));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.startQuiz(99L, model, authentication);

        assertThat(view).isEqualTo("redirect:/texts?error=Text tidak ditemukan");
    }

    @Test
    void submitQuizShouldRedirectWhenTextDeletedOrNotFound() {
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(achievementService.getAchievementsByUserId(authUser.getId())).thenReturn(List.of());
        when(quizService.submitQuiz(org.mockito.ArgumentMatchers.eq(99L), org.mockito.ArgumentMatchers.eq(authUser.getId().toString()), org.mockito.ArgumentMatchers.anyMap()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Text tidak ditemukan"));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.submitQuiz(99L, java.util.Map.of(), model, authentication);

        assertThat(view).isEqualTo("redirect:/texts?error=Teks bacaan telah dihapus oleh admin.");
    }

    private void setUserId(AuthUser authUser) {
        try {
            java.lang.reflect.Field idField = AuthUser.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(authUser, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
