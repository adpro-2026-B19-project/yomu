package id.ac.ui.cs.advprog.yomu.reading.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.dto.CreateTextRequest;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.TextService;
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
    private TextService textService;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TextController textController;

    @Test
    void getAllTextsShouldRenderListPage() {
        Text t = new Text();
        t.setTitle("A");
        when(textService.getAllTexts()).thenReturn(List.of(t));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getAllTexts(model);

        assertThat(view).isEqualTo("reading/texts");
        assertThat(model.getAttribute("texts")).isEqualTo(List.of(t));
    }

    @Test
    void getTextDetailShouldPropagateNotFoundForDraftOrMissingText() {
        when(textService.getPublishedTextById(99L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Text tidak ditemukan"));

        assertThatThrownBy(() -> textController.getTextDetail(99L, new ExtendedModelMap(), null))
                .isInstanceOf(ResponseStatusException.class);
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
    void createTextFormShouldPopulateCategoriesAndFormObject() {
        Category c = new Category();
        c.setName("Science");
        when(categoryRepository.findAll()).thenReturn(List.of(c));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.createTextForm(model);

        assertThat(view).isEqualTo("reading/create-text");
        assertThat(model.getAttribute("categories")).isEqualTo(List.of(c));
        assertThat(model.getAttribute("createTextRequest")).isNotNull();
    }

    @Test
    void createTextShouldThrowWhenCategoryNotFound() {
        CreateTextRequest request = new CreateTextRequest();
        request.setTitle("Title");
        request.setContent("Content");
        request.setCategoryId(10L);
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> textController.createText(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kategori tidak ditemukan");
    }

    @Test
    void createTextShouldDelegateToServiceAndRedirect() {
        Category category = new Category();
        category.setName("Tech");
        setCategoryId(category, 1L);
        CreateTextRequest request = new CreateTextRequest();
        request.setTitle("Title");
        request.setContent("Content");
        request.setCategoryId(1L);

        AuthUser authUser = new AuthUser("creator");
        setUserId(authUser);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));

        String view = textController.createText(request, authentication);

        assertThat(view).isEqualTo("redirect:/texts");
        verify(textService).createText("Title", "Content", 1L, authUser.getId().toString());
    }

    @Test
    void startQuizShouldRedirectAnonymousUserToLogin() {
        String view = textController.startQuiz(1L, new ExtendedModelMap(), null);
        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void startQuizShouldRenderQuizForPublishedText() {
        Text text = new Text();
        Question question = new Question();
        AuthUser authUser = new AuthUser("reader");
        setUserId(authUser);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(currentUserResolver.resolveUser(authentication)).thenReturn(Optional.of(authUser));
        when(textService.hasUserAttemptedQuiz(authUser.getId().toString(), 5L)).thenReturn(false);
        when(textService.getPublishedTextById(5L)).thenReturn(text);
        when(questionRepository.findByTextId(anyLong())).thenReturn(List.of(question));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.startQuiz(5L, model, authentication);

        assertThat(view).isEqualTo("reading/quiz");
        assertThat(model.getAttribute("text")).isEqualTo(text);
        assertThat(model.getAttribute("questions")).isEqualTo(List.of(question));
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

    private void setCategoryId(Category category, Long id) {
        try {
            java.lang.reflect.Field idField = Category.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
