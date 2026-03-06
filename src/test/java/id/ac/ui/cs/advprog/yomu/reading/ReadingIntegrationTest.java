package id.ac.ui.cs.advprog.yomu.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class ReadingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private TextRepository textRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanDatabase() {
        optionRepository.deleteAll();
        questionRepository.deleteAll();
        textRepository.deleteAll();
        categoryRepository.deleteAll();
        authRepository.deleteAll();
    }

    @Test
    void createTextPageShouldBeProtected() throws Exception {
        mockMvc.perform(get("/admin/texts/create"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminCreateTextWithQuestionsShouldPersistToDatabase() throws Exception {
        MockHttpSession session = loginAs("admin@yomu.com", "AdminPass123!", "ADMIN");

        Category techCategory = categoryRepository.save(new Category("Technology"));

        mockMvc.perform(post("/admin/texts")
                        .session(session)
                        .with(csrf())
                        .param("title", "Java 25 Features")
                        .param("content", "Java 25 is awesome because...")
                        .param("categoryId", techCategory.getId().toString())
                        // Pertanyaan Kuis
                        .param("question", "What version is this?")
                        .param("optionA", "Java 21")
                        .param("optionB", "Java 25")
                        .param("optionC", "Java 8")
                        .param("optionD", "Python")
                        .param("correct", "B"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/texts/create?success"));

        List<Text> texts = textRepository.findAll();
        assertThat(texts).hasSize(1);
        assertThat(texts.get(0).getTitle()).isEqualTo("Java 25 Features");
        assertThat(texts.get(0).getCategory().getName()).isEqualTo("Technology");

        assertThat(questionRepository.count()).isEqualTo(1);
        assertThat(questionRepository.findAll().get(0).getQuestion()).isEqualTo("What version is this?");

        assertThat(optionRepository.count()).isEqualTo(4);
    }

    @Test
    void studentCanViewListOfTexts() throws Exception {
        Category sportCat = categoryRepository.save(new Category("Sports"));
        textRepository.save(new Text("Manchester United Win", "Finally they won...", sportCat, "admin-id"));

        MockHttpSession session = loginAs("student@ui.ac.id", "Maba2025!", "USER");

        mockMvc.perform(get("/texts")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/texts"))
                .andExpect(model().attributeExists("texts"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Manchester United Win")));
    }

    @Test
    void studentCanViewTextDetail() throws Exception {
        Category scienceCat = categoryRepository.save(new Category("Science"));
        Text savedText = textRepository.save(new Text("Quantum Physics", "It is complex.", scienceCat, "admin-id"));

        MockHttpSession session = loginAs("student@ui.ac.id", "Maba2025!", "USER");

        mockMvc.perform(get("/texts/" + savedText.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/text-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quantum Physics")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("It is complex.")));
    }

    private MockHttpSession loginAs(String email, String rawPassword, String role) throws Exception {
        authRepository.save(new AuthUser("user-" + email.hashCode(), email, null, role, passwordEncoder.encode(rawPassword)));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}