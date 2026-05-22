package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.dto.CreateTextRequest;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.ITextService;
import id.ac.ui.cs.advprog.yomu.reading.service.IQuizService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTextControllerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ITextService textService;

    @Mock
    private IQuizService quizService;

    @InjectMocks
    private AdminTextController adminTextController;

    @Test
    void createTextPageShouldRenderCreateTextTemplateAndLoadCategories() {
        Category category = new Category("Digital Literacy");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminTextController.createTextPage(model);

        assertThat(view).isEqualTo("reading/create-text");
        assertThat(model.getAttribute("categories")).isEqualTo(List.of(category));
    }

    @Test
    void adminDashboardShouldRenderTextsAndCategoriesWithFilters() {
        Category category = new Category("News & Media");
        setId(category, 1L);

        Text text = new Text();
        text.setTitle("Sample Text");
        text.setCategory(category);
        text.setPublished(true);

        when(textService.getAllTextsAdmin(1L, true)).thenReturn(List.of(text));
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminTextController.adminDashboard(1L, true, model);

        assertThat(view).isEqualTo("reading/admin-dashboard");
        assertThat(model.getAttribute("texts")).isEqualTo(List.of(text));
        assertThat(model.getAttribute("categories")).isEqualTo(List.of(category));
        verify(textService).getAllTextsAdmin(1L, true);
    }

    @Test
    void createTextShouldSaveDraftTextAndRedirectToAdminDashboard() {
        CreateTextRequest request = new CreateTextRequest();
        request.setTitle("Mengapa Data Perlu Diperiksa");
        request.setContent("Data yang terlihat benar tetap perlu diverifikasi.");
        request.setCategoryId(7L);

        String view = adminTextController.createText(request);

        verify(textService).createText("Mengapa Data Perlu Diperiksa", "Data yang terlihat benar tetap perlu diverifikasi.", 7L, null);

        assertThat(view).isEqualTo("redirect:/admin/texts?success=created");
    }

    @Test
    void publishTextShouldRedirectWithSuccessWhenServiceSucceeds() {
        String view = adminTextController.publishText(10L);

        verify(textService).publishText(10L);
        assertThat(view).isEqualTo("redirect:/admin/texts?success=published");
    }

    @Test
    void publishTextShouldRedirectWithErrorWhenServiceRejectsInvalidContent() {
        org.mockito.Mockito.doThrow(new IllegalStateException("Cannot publish text without any questions."))
                .when(textService).publishText(10L);

        String view = adminTextController.publishText(10L);

        assertThat(view).isEqualTo("redirect:/admin/texts?error=Cannot publish text without any questions.");
    }

    @Test
    void deleteTextShouldCallServiceAndRedirect() {
        String view = adminTextController.deleteText(3L);

        verify(textService).deleteText(3L);
        assertThat(view).isEqualTo("redirect:/admin/texts?success=deleted");
    }

    @Test
    void addQuestionShouldCreateQuestionAndFourOptions() {
        String view = adminTextController.addQuestion(
                1L,
                "Apa inti teks tersebut?",
                "Opsi A",
                "Opsi B",
                "Opsi C",
                "Opsi D",
                "C"
        );

        verify(quizService).addQuestion(1L, "Apa inti teks tersebut?", "Opsi A", "Opsi B", "Opsi C", "Opsi D", "C");

        assertThat(view).isEqualTo("redirect:/admin/texts/1/questions");
    }

    @Test
    void manageQuestionsShouldRenderTextAndQuestionsWithOptionsSortedById() {
        Text text = new Text();
        text.setTitle("Reading Text");
        setId(text, 1L);

        Question question = new Question();
        setId(question, 5L);
        question.setText(text);

        Option later = new Option("B", false);
        setId(later, 20L);
        Option earlier = new Option("A", true);
        setId(earlier, 10L);
        question.setOptions(List.of(later, earlier));

        when(textService.getTextById(1L)).thenReturn(text);
        when(quizService.getQuestionsByTextId(1L)).thenReturn(List.of(question));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminTextController.manageQuestions(1L, model);

        assertThat(view).isEqualTo("reading/admin-manage-questions");
        assertThat(model.getAttribute("text")).isEqualTo(text);
        assertThat(model.getAttribute("questions")).isEqualTo(List.of(question));
        assertThat(question.getOptions()).containsExactly(earlier, later);
    }

    @Test
    void manageQuestionsShouldHandleQuestionWithoutOptions() {
        Text text = new Text();
        setId(text, 1L);

        Question question = new Question();
        question.setText(text);
        question.setOptions(null);

        when(textService.getTextById(1L)).thenReturn(text);
        when(quizService.getQuestionsByTextId(1L)).thenReturn(List.of(question));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminTextController.manageQuestions(1L, model);

        assertThat(view).isEqualTo("reading/admin-manage-questions");
        assertThat(question.getOptions()).isNull();
    }

    @Test
    void editQuestionShouldUpdateQuestionAndOptionsThenRedirect() {
        when(quizService.editQuestion(5L, "Pertanyaan baru", 11L, 12L, 13L, 14L, "Baru A", "Baru B", "Baru C", "Baru D", "D")).thenReturn(1L);

        String view = adminTextController.editQuestion(
                5L,
                "Pertanyaan baru",
                11L,
                12L,
                13L,
                14L,
                "Baru A",
                "Baru B",
                "Baru C",
                "Baru D",
                "D"
        );

        verify(quizService).editQuestion(5L, "Pertanyaan baru", 11L, 12L, 13L, 14L, "Baru A", "Baru B", "Baru C", "Baru D", "D");
        assertThat(view).isEqualTo("redirect:/admin/texts/1/questions");
    }

    @Test
    void deleteQuestionShouldDeleteAndRedirectToTextQuestions() {
        when(quizService.deleteQuestion(5L)).thenReturn(1L);
        String view = adminTextController.deleteQuestion(5L);
        verify(quizService).deleteQuestion(5L);
        assertThat(view).isEqualTo("redirect:/admin/texts/1/questions");
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
