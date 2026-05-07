package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.dto.CreateTextRequest;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import id.ac.ui.cs.advprog.yomu.reading.service.TextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTextControllerTest {

    @Mock
    private TextRepository textRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private TextService textService;

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
        Category category = new Category("Science");
        setId(category, 7L);

        CreateTextRequest request = new CreateTextRequest();
        request.setTitle("Mengapa Data Perlu Diperiksa");
        request.setContent("Data yang terlihat benar tetap perlu diverifikasi.");
        request.setCategoryId(7L);

        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));

        String view = adminTextController.createText(request);

        ArgumentCaptor<Text> captor = ArgumentCaptor.forClass(Text.class);
        verify(textRepository).save(captor.capture());

        Text savedText = captor.getValue();
        assertThat(savedText.getTitle()).isEqualTo("Mengapa Data Perlu Diperiksa");
        assertThat(savedText.getContent()).isEqualTo("Data yang terlihat benar tetap perlu diverifikasi.");
        assertThat(savedText.getCategory()).isEqualTo(category);
        assertThat(savedText.isPublished()).isFalse();
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
        Text text = new Text();
        text.setTitle("Reading Text");
        setId(text, 1L);

        when(textRepository.findById(1L)).thenReturn(Optional.of(text));

        String view = adminTextController.addQuestion(
                1L,
                "Apa inti teks tersebut?",
                "Opsi A",
                "Opsi B",
                "Opsi C",
                "Opsi D",
                "C"
        );

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(questionCaptor.capture());

        Question savedQuestion = questionCaptor.getValue();
        assertThat(savedQuestion.getQuestion()).isEqualTo("Apa inti teks tersebut?");
        assertThat(savedQuestion.getText()).isEqualTo(text);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Option>> optionsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(optionRepository).saveAll(optionsCaptor.capture());

        List<Option> options = new ArrayList<>();
        optionsCaptor.getValue().forEach(options::add);

        assertThat(options).hasSize(4);
        assertThat(options).extracting(Option::getText)
                .containsExactly("Opsi A", "Opsi B", "Opsi C", "Opsi D");
        assertThat(options.stream().filter(Option::isCorrect)).singleElement()
                .extracting(Option::getText).isEqualTo("Opsi C");

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

        when(textRepository.findById(1L)).thenReturn(Optional.of(text));
        when(questionRepository.findByTextId(1L)).thenReturn(List.of(question));

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

        when(textRepository.findById(1L)).thenReturn(Optional.of(text));
        when(questionRepository.findByTextId(1L)).thenReturn(List.of(question));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminTextController.manageQuestions(1L, model);

        assertThat(view).isEqualTo("reading/admin-manage-questions");
        assertThat(question.getOptions()).isNull();
    }

    @Test
    void editQuestionShouldUpdateQuestionAndOptionsThenRedirect() {
        Text text = new Text();
        setId(text, 1L);

        Question question = new Question();
        setId(question, 5L);
        question.setText(text);

        Option optionA = new Option("Old A", true);
        setId(optionA, 11L);
        Option optionB = new Option("Old B", false);
        setId(optionB, 12L);
        Option optionC = new Option("Old C", false);
        setId(optionC, 13L);
        Option optionD = new Option("Old D", false);
        setId(optionD, 14L);
        question.setOptions(List.of(optionA, optionB, optionC, optionD));

        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));

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

        assertThat(question.getQuestion()).isEqualTo("Pertanyaan baru");
        verify(questionRepository).save(question);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Option>> optionsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(optionRepository).saveAll(optionsCaptor.capture());
        List<Option> savedOptions = new ArrayList<>();
        optionsCaptor.getValue().forEach(savedOptions::add);
        assertThat(savedOptions).containsExactlyInAnyOrder(optionA, optionB, optionC, optionD);
        assertThat(optionA.getText()).isEqualTo("Baru A");
        assertThat(optionA.isCorrect()).isFalse();
        assertThat(optionD.getText()).isEqualTo("Baru D");
        assertThat(optionD.isCorrect()).isTrue();
        assertThat(view).isEqualTo("redirect:/admin/texts/1/questions");
    }

    @Test
    void editQuestionShouldThrowWhenOptionIdIsMissing() {
        Text text = new Text();
        setId(text, 1L);

        Question question = new Question();
        question.setText(text);
        Option option = new Option("Only option", true);
        setId(option, 11L);
        question.setOptions(List.of(option));

        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminTextController.editQuestion(
                        5L,
                        "Pertanyaan baru",
                        11L,
                        12L,
                        13L,
                        14L,
                        "A",
                        "B",
                        "C",
                        "D",
                        "A"
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Option tidak ditemukan");
    }

    @Test
    void deleteQuestionShouldDeleteAndRedirectToTextQuestions() {
        Text text = new Text();
        setId(text, 1L);

        Question question = new Question();
        question.setText(text);

        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));

        String view = adminTextController.deleteQuestion(5L);

        verify(questionRepository).deleteById(5L);
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
