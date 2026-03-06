package id.ac.ui.cs.advprog.yomu.reading.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OptionTest {

    private Option option;
    private Question question;

    @BeforeEach
    void setUp() {
        option = new Option();
        question = new Question();
        question.setQuestion("Test Question");
    }

    @Test
    void testConstructorWithArgs() {
        Option manualOption = new Option("Jawaban A", true);
        assertEquals("Jawaban A", manualOption.getText());
        assertTrue(manualOption.isCorrect());
    }

    @Test
    void testSetAndGetText() {
        String text = "Pilihan Ganda";
        option.setText(text);
        assertEquals(text, option.getText());
    }

    @Test
    void testSetAndIsCorrect() {
        option.setCorrect(true);
        assertTrue(option.isCorrect());

        option.setCorrect(false);
        assertFalse(option.isCorrect());
    }

    @Test
    void testSetAndGetQuestion() {
        option.setQuestion(question);
        assertEquals(question, option.getQuestion());
        assertEquals("Test Question", option.getQuestion().getQuestion());
    }
}