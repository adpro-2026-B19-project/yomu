package id.ac.ui.cs.advprog.yomu.config;

import id.ac.ui.cs.advprog.yomu.reading.model.*;
import id.ac.ui.cs.advprog.yomu.reading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final TextRepository textRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;

    @Override
    public void run(String... args) {

        if (categoryRepository.count() > 0) {
            return;
        }

        Category tech = new Category();
        tech.setName("Technology");

        Category science = new Category();
        science.setName("Science");

        Category history = new Category();
        history.setName("History");

        categoryRepository.save(tech);
        categoryRepository.save(science);
        categoryRepository.save(history);

        Text text1 = new Text();
        text1.setTitle("The Rise of Artificial Intelligence");
        text1.setContent("Artificial Intelligence is transforming industries...");
        text1.setCategory(tech);

        Text text2 = new Text();
        text2.setTitle("The Solar System");
        text2.setContent("The solar system consists of the Sun and planets...");
        text2.setCategory(science);

        Text text3 = new Text();
        text3.setTitle("Ancient Civilizations");
        text3.setContent("Ancient civilizations like Egypt and Mesopotamia...");
        text3.setCategory(history);

        textRepository.save(text1);
        textRepository.save(text2);
        textRepository.save(text3);

        Question q1 = new Question();
        q1.setText(text1);
        q1.setQuestion("What is the main topic of the article?");
        questionRepository.save(q1);

        Option o1 = new Option();
        o1.setQuestion(q1);
        o1.setText("Artificial Intelligence");
        o1.setCorrect(true);

        Option o2 = new Option();
        o2.setQuestion(q1);
        o2.setText("Cooking Recipes");
        o2.setCorrect(false);

        Option o3 = new Option();
        o3.setQuestion(q1);
        o3.setText("Football History");
        o3.setCorrect(false);

        Option o4 = new Option();
        o4.setQuestion(q1);
        o4.setText("Travel Guide");
        o4.setCorrect(false);

        optionRepository.save(o1);
        optionRepository.save(o2);
        optionRepository.save(o3);
        optionRepository.save(o4);
    }
}