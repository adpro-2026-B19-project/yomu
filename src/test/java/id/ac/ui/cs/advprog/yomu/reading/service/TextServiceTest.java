package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextServiceTest {

    @Mock
    private TextRepository textRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TextService textService;

    private Text text;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Teknologi");
        text = new Text("Judul Test", "Konten Test", category, "user-123");
    }

    @Test
    void testGetAllTexts() {
        when(textRepository.findByPublishedTrue()).thenReturn(List.of(text));

        List<Text> result = textService.getAllTexts();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Judul Test", result.get(0).getTitle());
        verify(textRepository, times(1)).findByPublishedTrue();
    }

    @Test
    void testGetTextById_Found() {
        when(textRepository.findById(1L)).thenReturn(Optional.of(text));

        Text result = textService.getTextById(1L);

        assertNotNull(result);
        assertEquals("Judul Test", result.getTitle());
    }

    @Test
    void testCreateText() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(textRepository.save(any(Text.class))).thenReturn(text);

        Text result = textService.createText("Judul Baru", "Isi Baru", 1L, "user-1");

        assertNotNull(result);
        assertEquals("Judul Test", result.getTitle());
        verify(textRepository, times(1)).save(any(Text.class));
    }
}