package id.ac.ui.cs.advprog.yomu.reading.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.reading.dto.CreateTextRequest;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

@ExtendWith(MockitoExtension.class)
class TextControllerTest {

    @Mock
    private TextRepository textRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TextController textController;

    @Test
    void getAllTextsShouldRenderListPage() {
        Text t = new Text();
        t.setTitle("A");
        when(textRepository.findAll()).thenReturn(List.of(t));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getAllTexts(model);

        assertThat(view).isEqualTo("reading/texts");
        assertThat(model.getAttribute("texts")).isEqualTo(List.of(t));
    }

    @Test
    void getTextDetailShouldThrowWhenNotFound() {
        when(textRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> textController.getTextDetail(99L, new ExtendedModelMap()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Text tidak ditemukan");
    }

    @Test
    void getTextDetailShouldRenderWhenFound() {
        Text t = new Text();
        t.setTitle("Hello");
        when(textRepository.findById(1L)).thenReturn(Optional.of(t));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = textController.getTextDetail(1L, model);

        assertThat(view).isEqualTo("reading/text-detail");
        assertThat(model.getAttribute("text")).isEqualTo(t);
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

        assertThatThrownBy(() -> textController.createText(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kategori tidak ditemukan");
    }

    @Test
    void createTextShouldPersistAndRedirect() {
        Category category = new Category();
        category.setName("Tech");
        CreateTextRequest request = new CreateTextRequest();
        request.setTitle("Title");
        request.setContent("Content");
        request.setCategoryId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        String view = textController.createText(request);

        assertThat(view).isEqualTo("redirect:/texts");
        ArgumentCaptor<Text> captor = ArgumentCaptor.forClass(Text.class);
        verify(textRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Title");
        assertThat(captor.getValue().getContent()).isEqualTo("Content");
        assertThat(captor.getValue().getCategory()).isEqualTo(category);
    }
}
