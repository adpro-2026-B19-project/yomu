package id.ac.ui.cs.advprog.yomu.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.repository.AchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserMissionProgressRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserStatisticRepository;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanJoinRequestRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanQuizScoreEventRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.LeagueSeasonRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class Milestone75KalfinIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TextRepository textRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserMissionProgressRepository userMissionProgressRepository;

    @Autowired
    private UserStatisticRepository userStatisticRepository;

    @Autowired
    private ClanRepository clanRepository;

    @Autowired
    private ClanMemberRepository clanMemberRepository;

    @Autowired
    private ClanJoinRequestRepository clanJoinRequestRepository;

    @Autowired
    private ClanQuizScoreEventRepository clanQuizScoreEventRepository;

    @Autowired
    private TierRepository tierRepository;

    @Autowired
    private LeagueSeasonRepository leagueSeasonRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanDatabase() {
        clanQuizScoreEventRepository.deleteAll();
        clanMemberRepository.deleteAll();
        clanJoinRequestRepository.deleteAll();
        clanRepository.deleteAll();
        leagueSeasonRepository.deleteAll();
        tierRepository.deleteAll();

        userAchievementRepository.deleteAll();
        userMissionProgressRepository.deleteAll();
        userStatisticRepository.deleteAll();
        achievementRepository.deleteAll();
        dailyMissionRepository.deleteAll();

        quizAttemptRepository.deleteAll();
        optionRepository.deleteAll();
        questionRepository.deleteAll();
        textRepository.deleteAll();
        categoryRepository.deleteAll();

        authRepository.deleteAll();
    }

    @Test
    void fullMilestone75KalfinFlowShouldPass() throws Exception {
        MockHttpSession adminSession = loginAs("admin@yomu.test", "AdminPass1!", AuthRole.ADMIN, "Admin");
        MockHttpSession alphaLeaderSession = loginAs("alpha@yomu.test", "AlphaPass1!", AuthRole.USER, "Alpha Leader");
        MockHttpSession alphaMemberSession = loginAs("alpha-member@yomu.test", "AlphaMember1!", AuthRole.USER, "Alpha Member");
        MockHttpSession betaLeaderSession = loginAs("beta@yomu.test", "BetaPass1!", AuthRole.USER, "Beta Leader");
        MockHttpSession gammaLeaderSession = loginAs("gamma@yomu.test", "GammaPass1!", AuthRole.USER, "Gamma Leader");
        MockHttpSession deltaLeaderSession = loginAs("delta@yomu.test", "DeltaPass1!", AuthRole.USER, "Delta Leader");

        Category category = categoryRepository.save(new Category("Fact Check"));
        DailyMission mission = dailyMissionRepository.save(DailyMission.builder()
                .title("Complete 1 reading today")
                .targetCount(1)
                .activeDate(LocalDate.now())
                .primary(true)
                .build());
        Achievement achievement = achievementRepository.save(Achievement.builder()
                .name("First Reading")
                .milestone("Complete your first reading")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(1)
                .build());

        long textId = createAndPublishText(adminSession, category.getId());
        long correctOptionId = loadCorrectOptionId(textId);

        UUID alphaLeaderId = authRepository.findByEmail("alpha@yomu.test").orElseThrow().getId();
        UUID alphaMemberId = authRepository.findByEmail("alpha-member@yomu.test").orElseThrow().getId();
        UUID betaLeaderId = authRepository.findByEmail("beta@yomu.test").orElseThrow().getId();
        UUID gammaLeaderId = authRepository.findByEmail("gamma@yomu.test").orElseThrow().getId();
        UUID deltaLeaderId = authRepository.findByEmail("delta@yomu.test").orElseThrow().getId();

        createClan(alphaLeaderSession, "Alpha Clan");
        createClan(betaLeaderSession, "Beta Clan");
        createClan(gammaLeaderSession, "Gamma Clan");
        createClan(deltaLeaderSession, "Delta Clan");

        UUID alphaClanId = clanRepository.findAllForListing().stream()
                .filter(clan -> clan.getName().equals("Alpha Clan"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/clans/" + alphaClanId + "/join")
                        .session(alphaMemberSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans/" + alphaClanId));

        UUID joinRequestId = clanJoinRequestRepository.findAll().getFirst().getId();
        mockMvc.perform(post("/clans/" + alphaClanId + "/requests/" + joinRequestId + "/decision")
                        .session(alphaLeaderSession)
                        .with(csrf())
                        .param("action", "approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans/" + alphaClanId));

        submitPerfectQuiz(alphaLeaderSession, textId, correctOptionId);
        submitPerfectQuiz(alphaMemberSession, textId, correctOptionId);
        submitPerfectQuiz(betaLeaderSession, textId, correctOptionId);
        submitPerfectQuiz(gammaLeaderSession, textId, correctOptionId);
        submitPerfectQuiz(deltaLeaderSession, textId, correctOptionId);

        assertThat(userStatisticRepository.findByUserId(alphaMemberId)).isPresent();
        assertThat(userMissionProgressRepository.findByUserIdAndMissionId(alphaLeaderId, mission.getId())).isPresent();
        assertThat(userMissionProgressRepository.findByUserIdAndMissionId(alphaMemberId, mission.getId())).isPresent();
        assertThat(userAchievementRepository.findByUserId(alphaMemberId))
                .extracting(userAchievement -> userAchievement.getAchievement().getId())
                .contains(achievement.getId());

        mockMvc.perform(post("/achievements/api/user/toggle-display/" + achievement.getId())
                        .session(alphaMemberSession)
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/league/leaderboard/BRONZE").session(alphaLeaderSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].clanName").value("Alpha Clan"))
                .andExpect(jsonPath("$.entries[0].baseScore").value(200.0))
                .andExpect(jsonPath("$.entries[0].score").value(240.0))
                .andExpect(jsonPath("$.entries[0].activeModifiers[0].code").value("PRODUCTIVITY_BUFF"))
                .andExpect(jsonPath("$.totalEntries").value(4));

        mockMvc.perform(get("/players/" + alphaMemberId).session(alphaLeaderSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("First Reading")))
                .andExpect(content().string(containsString("Alpha Clan")));

        mockMvc.perform(post("/admin/league/season/end")
                        .session(adminSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leaderboard?tier=BRONZE"));

        var promotedAlphaClan = clanRepository.findAllForListing().stream()
                .filter(clan -> clan.getName().equals("Alpha Clan"))
                .findFirst()
                .orElseThrow();
        assertThat(promotedAlphaClan.getTier().getCode()).isEqualTo(TierCode.SILVER);
        assertThat(clanQuizScoreEventRepository.findAll()).hasSize(5);
    }

    private MockHttpSession loginAs(String email, String rawPassword, AuthRole role, String displayName) throws Exception {
        authRepository.save(new AuthUser(
                "user-" + email.hashCode(),
                email,
                null,
                displayName,
                passwordEncoder.encode(rawPassword),
                role
        ));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", email)
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private long createAndPublishText(MockHttpSession adminSession, long categoryId) throws Exception {
        mockMvc.perform(post("/admin/texts")
                        .session(adminSession)
                        .with(csrf())
                        .param("title", "Hoax Verification 101")
                        .param("content", "Always check the source before sharing.")
                        .param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/texts?success=created"));

        Text text = textRepository.findAll().getFirst();
        mockMvc.perform(post("/admin/texts/" + text.getId() + "/questions")
                        .session(adminSession)
                        .with(csrf())
                        .param("questionText", "What should you verify first?")
                        .param("optionA", "The source")
                        .param("optionB", "The font size")
                        .param("optionC", "The comment count")
                        .param("optionD", "The color palette")
                        .param("correctOption", "A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/texts/" + text.getId() + "/questions"));

        mockMvc.perform(post("/admin/texts/" + text.getId() + "/publish")
                        .session(adminSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/texts?success=published"));

        return text.getId();
    }

    private long loadCorrectOptionId(long textId) {
        Question question = questionRepository.findByTextId(textId).getFirst();
        return optionRepository.findAll().stream()
                .filter(option -> option.getQuestion().getId().equals(question.getId()) && option.isCorrect())
                .findFirst()
                .map(Option::getId)
                .orElseThrow();
    }

    private void createClan(MockHttpSession session, String clanName) throws Exception {
        mockMvc.perform(post("/clans")
                        .session(session)
                        .with(csrf())
                        .param("name", clanName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans"));
    }

    private void submitPerfectQuiz(MockHttpSession session, long textId, long correctOptionId) throws Exception {
        Question question = questionRepository.findByTextId(textId).getFirst();
        mockMvc.perform(post("/texts/" + textId + "/quiz/submit")
                        .session(session)
                        .with(csrf())
                        .param("question_" + question.getId(), String.valueOf(correctOptionId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kuis Selesai!")));
    }
}
