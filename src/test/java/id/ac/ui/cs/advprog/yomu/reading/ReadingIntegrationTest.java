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

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private id.ac.ui.cs.advprog.yomu.reading.service.ITextService textService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanDatabase() {
        quizAttemptRepository.deleteAll();
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
    void nonAdminShouldNotAccessAdminCreatePage() throws Exception {
        MockHttpSession session = loginAs("student@ui.ac.id", "Maba2025!", AuthRole.USER);

        mockMvc.perform(get("/admin/texts/create").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void learnerFacingCreateRouteShouldNoLongerExist() throws Exception {
        MockHttpSession session = loginAs("student-route@ui.ac.id", "Maba2025!", AuthRole.USER);

        mockMvc.perform(get("/texts/create").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanCreateDraftAndAddQuestions() throws Exception {
        MockHttpSession session = loginAs("admin@yomu.com", "AdminPass123!", AuthRole.ADMIN);
        Category techCategory = categoryRepository.save(new Category("Technology"));

        mockMvc.perform(post("/admin/texts")
                        .session(session)
                        .with(csrf())
                        .param("title", "Java 25 Features")
                        .param("content", "Java 25 is awesome because...")
                        .param("categoryId", techCategory.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/texts?success=created"));

        List<Text> texts = textRepository.findAll();
        assertThat(texts).hasSize(1);
        assertThat(texts.get(0).getTitle()).isEqualTo("Java 25 Features");
        assertThat(texts.get(0).isPublished()).isFalse();

        Long textId = texts.get(0).getId();
        mockMvc.perform(post("/admin/texts/" + textId + "/questions")
                        .session(session)
                        .with(csrf())
                        .param("questionText", "What version is this?")
                        .param("optionA", "Java 21")
                        .param("optionB", "Java 25")
                        .param("optionC", "Java 8")
                        .param("optionD", "Python")
                        .param("correctOption", "B"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/texts/" + textId + "/questions"));

        assertThat(questionRepository.count()).isEqualTo(1);
        assertThat(questionRepository.findAll().get(0).getQuestion()).isEqualTo("What version is this?");
        assertThat(optionRepository.count()).isEqualTo(4);
    }

    @Test
    void studentCanViewListOfPublishedTexts() throws Exception {
        Category sportCat = categoryRepository.save(new Category("Sports"));
        Text text = new Text("Manchester United Win", "Finally they won...", sportCat, "admin-id");
        text.setPublished(true);
        textRepository.save(text);

        MockHttpSession session = loginAs("student@ui.ac.id", "Maba2025!", AuthRole.USER);

        mockMvc.perform(get("/texts").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/texts"))
                .andExpect(model().attributeExists("texts"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Manchester United Win")));
    }

    @Test
    void studentCannotViewDraftTextDetail() throws Exception {
        Category scienceCat = categoryRepository.save(new Category("Science"));
        Text savedText = textRepository.save(new Text("Quantum Physics", "It is complex.", scienceCat, "admin-id"));

        MockHttpSession session = loginAs("student@ui.ac.id", "Maba2025!", AuthRole.USER);

        mockMvc.perform(get("/texts/" + savedText.getId()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/texts?error=Text tidak ditemukan"));
    }

    @Test
    void studentCanViewPublishedTextDetail() throws Exception {
        Category scienceCat = categoryRepository.save(new Category("Science"));
        Text savedText = new Text("Quantum Physics", "It is complex.", scienceCat, "admin-id");
        savedText.setPublished(true);
        savedText = textRepository.save(savedText);

        MockHttpSession session = loginAs("student@ui.ac.id", "Maba2025!", AuthRole.USER);

        mockMvc.perform(get("/texts/" + savedText.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/text-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quantum Physics")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("It is complex.")));
    }

    @Test
    void adminCanEditExistingQuestion() throws Exception {
        MockHttpSession session = loginAs("admin@yomu.com", "AdminPass123!", AuthRole.ADMIN);
        Category category = categoryRepository.save(new Category("History"));
        Text text = textRepository.save(new Text("Ancient Rome", "Roma...", category, "admin-id"));

        Question question = new Question();
        question.setText(text);
        question.setQuestion("Old question");
        question = questionRepository.save(question);
        Long questionId = question.getId();

        Option optA = optionRepository.save(createOption(question, "A1", true));
        Option optB = optionRepository.save(createOption(question, "B1", false));
        Option optC = optionRepository.save(createOption(question, "C1", false));
        Option optD = optionRepository.save(createOption(question, "D1", false));

        mockMvc.perform(post("/admin/texts/questions/" + question.getId() + "/edit")
                        .session(session)
                        .with(csrf())
                        .param("questionText", "Updated question")
                        .param("optionAId", optA.getId().toString())
                        .param("optionBId", optB.getId().toString())
                        .param("optionCId", optC.getId().toString())
                        .param("optionDId", optD.getId().toString())
                        .param("optionA", "A2")
                        .param("optionB", "B2")
                        .param("optionC", "C2")
                        .param("optionD", "D2")
                        .param("correctOption", "C"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/texts/" + text.getId() + "/questions"));

        Question updatedQuestion = questionRepository.findById(questionId).orElseThrow();
        assertThat(updatedQuestion.getQuestion()).isEqualTo("Updated question");
        List<Option> options = optionRepository.findAll().stream()
                .filter(option -> option.getQuestion().getId().equals(questionId))
                .toList();
        assertThat(options).extracting(Option::getText).containsExactlyInAnyOrder("A2", "B2", "C2", "D2");
        assertThat(options.stream().filter(Option::isCorrect)).singleElement().extracting(Option::getText).isEqualTo("C2");
    }

    @Test
    void studentCanViewPaginatedPublishedTexts() throws Exception {
        Category sportCat = categoryRepository.save(new Category("Sports"));
        for (int i = 1; i <= 10; i++) {
            Text text = new Text("Text " + i, "Content " + i, sportCat, "admin-id");
            text.setPublished(true);
            textRepository.save(text);
        }

        MockHttpSession session = loginAs("student-paged@ui.ac.id", "Maba2025!", AuthRole.USER);

        mockMvc.perform(get("/texts").session(session).param("page", "0").param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/texts"))
                .andExpect(model().attributeExists("texts"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("hasNext", true))
                .andExpect(model().attribute("hasPrevious", false));

        mockMvc.perform(get("/texts").session(session).param("page", "1").param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("reading/texts"))
                .andExpect(model().attributeExists("texts"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(model().attribute("hasPrevious", true));
    }

    @Test
    void quizSubmissionShouldFailGracefullyIfTextDeleted() throws Exception {
        Category techCat = categoryRepository.save(new Category("Technology"));
        Text text = new Text("Java 25", "Java is fun.", techCat, "admin-id");
        text.setPublished(true);
        text = textRepository.save(text);

        Question q = new Question();
        q.setText(text);
        q.setQuestion("Is Java fun?");
        q = questionRepository.save(q);

        Option opt = new Option("Yes", true);
        opt.setQuestion(q);
        optionRepository.save(opt);

        MockHttpSession session = loginAs("student-submit@ui.ac.id", "Maba2025!", AuthRole.USER);

        textService.deleteText(text.getId());

        mockMvc.perform(post("/texts/" + text.getId() + "/quiz/submit")
                        .session(session)
                        .with(csrf())
                        .param("question_" + q.getId(), opt.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/texts?error=Teks bacaan telah dihapus oleh admin."));
    }

    private MockHttpSession loginAs(String email, String rawPassword, AuthRole role) throws Exception {
        authRepository.save(new AuthUser("user-" + email.hashCode(), email, null, role.name(), passwordEncoder.encode(rawPassword), role));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", email)
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private Option createOption(Question question, String text, boolean correct) {
        Option option = new Option(text, correct);
        option.setQuestion(question);
        return option;
    }
}
