package id.ac.ui.cs.advprog.yomu.league;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequestStatus;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanJoinRequestRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanQuizScoreEventRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
import java.time.LocalDateTime;
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
class ClanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRepository authRepository;

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

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanDatabase() {
        clanQuizScoreEventRepository.deleteAll();
        clanMemberRepository.deleteAll();
        clanJoinRequestRepository.deleteAll();
        clanRepository.deleteAll();
        tierRepository.deleteAll();
        authRepository.deleteAll();
    }

    @Test
    void clanPageShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/clans"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void createClanFromWebFormShouldPersistData() throws Exception {
        MockHttpSession session = loginAs("kalfin@example.com", "KalfinPass1!");

        mockMvc.perform(post("/clans")
                        .session(session)
                        .with(csrf())
                        .param("name", "Code Masters"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans"));

        mockMvc.perform(get("/clans").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Code Masters")));

        assertThat(clanRepository.count()).isEqualTo(1);
        assertThat(clanMemberRepository.count()).isEqualTo(1);
        assertThat(tierRepository.findByCode(TierCode.BRONZE)).isPresent();
    }

    @Test
    void createAndListClanViaApiShouldWork() throws Exception {
        MockHttpSession session = loginAs("api-user@example.com", "ApiUserPass1!");

        mockMvc.perform(post("/api/clans")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bronze Riders"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bronze Riders"))
                .andExpect(jsonPath("$.tier").value("BRONZE"))
                .andExpect(jsonPath("$.memberCount").value(1));

        mockMvc.perform(get("/api/clans").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bronze Riders"))
                .andExpect(jsonPath("$[0].tier").value("BRONZE"));
    }

    @Test
    void userCanRequestToJoinAndLeaderCanApprove() throws Exception {
        MockHttpSession leaderSession = loginAs("leader@example.com", "LeaderPass1!");
        MockHttpSession learnerSession = loginAs("learner@example.com", "LearnerPass1!");

        mockMvc.perform(post("/clans")
                        .session(leaderSession)
                        .with(csrf())
                        .param("name", "Algebra Guild"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans"));

        var clan = clanRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/clans/" + clan.getId() + "/join")
                        .session(learnerSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans/" + clan.getId()));

        assertThat(clanJoinRequestRepository.count()).isEqualTo(1);
        assertThat(clanJoinRequestRepository.findAll().getFirst().getStatus()).isEqualTo(ClanJoinRequestStatus.PENDING);

        var joinRequest = clanJoinRequestRepository.findAll().getFirst();

        mockMvc.perform(post("/clans/" + clan.getId() + "/requests/" + joinRequest.getId() + "/decision")
                        .session(leaderSession)
                        .with(csrf())
                        .param("action", "approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans/" + clan.getId()));

        assertThat(clanMemberRepository.count()).isEqualTo(2);
        assertThat(clanJoinRequestRepository.findAll().getFirst().getStatus()).isEqualTo(ClanJoinRequestStatus.APPROVED);
    }

    @Test
    void quizCompletionEventShouldUpdateBronzeLeaderboard() throws Exception {
        MockHttpSession leaderSession = loginAs("leader-2@example.com", "LeaderPass1!");
        MockHttpSession learnerSession = loginAs("learner-2@example.com", "LearnerPass1!");

        mockMvc.perform(post("/clans")
                        .session(leaderSession)
                        .with(csrf())
                        .param("name", "Logic Guild"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans"));

        var clan = clanRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/clans/" + clan.getId() + "/join")
                        .session(learnerSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        var joinRequest = clanJoinRequestRepository.findAll().getFirst();
        mockMvc.perform(post("/clans/" + clan.getId() + "/requests/" + joinRequest.getId() + "/decision")
                        .session(leaderSession)
                        .with(csrf())
                        .param("action", "approve"))
                .andExpect(status().is3xxRedirection());

        var learnerUser = authRepository.findByEmail("learner-2@example.com").orElseThrow();

        mockMvc.perform(post("/api/league/events/quiz-completions")
                        .session(leaderSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"%s",
                                  "userId":"%s",
                                  "textId":"%s",
                                  "score":9.5,
                                  "accuracy":0.95,
                                  "completedAt":"%s"
                                }
                                """.formatted(
                                UUID.randomUUID(),
                                learnerUser.getId(),
                                UUID.randomUUID(),
                                LocalDateTime.now().minusMinutes(1)
                        )))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/league/leaderboard/bronze").session(leaderSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clanName").value("Logic Guild"))
                .andExpect(jsonPath("$[0].score").value(9.5));
    }

    @Test
    void publicProfilePageShouldShowPublicDataWithoutEmail() throws Exception {
        MockHttpSession leaderSession = loginAs("leader-3@example.com", "LeaderPass1!");
        MockHttpSession learnerSession = loginAs("learner-3@example.com", "LearnerPass1!");

        mockMvc.perform(post("/clans")
                        .session(leaderSession)
                        .with(csrf())
                        .param("name", "Geometry Guild"))
                .andExpect(status().is3xxRedirection());

        var clan = clanRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/clans/" + clan.getId() + "/join")
                        .session(learnerSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        var joinRequest = clanJoinRequestRepository.findAll().getFirst();
        mockMvc.perform(post("/clans/" + clan.getId() + "/requests/" + joinRequest.getId() + "/decision")
                        .session(leaderSession)
                        .with(csrf())
                        .param("action", "approve"))
                .andExpect(status().is3xxRedirection());

        var learnerUser = authRepository.findByEmail("learner-3@example.com").orElseThrow();

        mockMvc.perform(post("/api/league/events/quiz-completions")
                        .session(leaderSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"%s",
                                  "userId":"%s",
                                  "textId":"%s",
                                  "score":8.0,
                                  "accuracy":0.8,
                                  "completedAt":"%s"
                                }
                                """.formatted(
                                UUID.randomUUID(),
                                learnerUser.getId(),
                                UUID.randomUUID(),
                                LocalDateTime.now().minusMinutes(1)
                        )))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/players/" + learnerUser.getId()).session(leaderSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public Quiz Stats")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(learnerUser.getUsername())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geometry Guild")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("learner-3@example.com")
                )));
    }

    private MockHttpSession loginAs(String email, String rawPassword) throws Exception {
        authRepository.save(new AuthUser("user-" + email.hashCode(), email, null, "user", passwordEncoder.encode(rawPassword)));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", email)
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
