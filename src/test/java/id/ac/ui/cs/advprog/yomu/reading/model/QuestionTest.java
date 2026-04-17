package id.ac.ui.cs.advprog.yomu.reading.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private Question question;

    @BeforeEach
    void setUp() {
        question = new Question();
    }

    @Test
    void testSetAndGetId() {
        Long id = 123L;
        question.setId(id);
        assertEquals(id, question.getId());
    }

    @Test
    void testSetAndGetQuestionText() {
        String text = "Apa ibukota Indonesia?";
        question.setQuestion(text);
        assertEquals(text, question.getQuestion());
    }

    @Test
    void testSetAndGetTextRelation() {
        Text dummyText = new Text();
        dummyText.setTitle("Dummy Article");

        question.setText(dummyText);

        assertNotNull(question.getText());
        assertEquals("Dummy Article", question.getText().getTitle());
    }

    @Test
    void testSetAndGetOptionsList() {
        List<Option> options = new ArrayList<>();
        Option optA = new Option();
        optA.setText("Jakarta");
        options.add(optA);

        question.setOptions(options);

        assertNotNull(question.getOptions());
        assertEquals(1, question.getOptions().size());
        assertEquals("Jakarta", question.getOptions().get(0).getText());
    }
}