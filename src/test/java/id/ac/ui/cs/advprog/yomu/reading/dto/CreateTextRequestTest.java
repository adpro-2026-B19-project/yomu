package id.ac.ui.cs.advprog.yomu.reading.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTextRequestTest {

    @Test
    void gettersShouldReturnAssignedValues() {
        CreateTextRequest request = new CreateTextRequest();

        request.setTitle("Judul");
        request.setContent("Konten");
        request.setCategoryId(3L);
        request.setQuestion("Pertanyaan");
        request.setOptionA("A");
        request.setOptionB("B");
        request.setOptionC("C");
        request.setOptionD("D");
        request.setCorrect("C");

        assertThat(request.getTitle()).isEqualTo("Judul");
        assertThat(request.getContent()).isEqualTo("Konten");
        assertThat(request.getCategoryId()).isEqualTo(3L);
        assertThat(request.getQuestion()).isEqualTo("Pertanyaan");
        assertThat(request.getOptionA()).isEqualTo("A");
        assertThat(request.getOptionB()).isEqualTo("B");
        assertThat(request.getOptionC()).isEqualTo("C");
        assertThat(request.getOptionD()).isEqualTo("D");
        assertThat(request.getCorrect()).isEqualTo("C");
    }
}
