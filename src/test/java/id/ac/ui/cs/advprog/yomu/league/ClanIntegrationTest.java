package id.ac.ui.cs.advprog.yomu.league;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
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

        mockMvc.perform(get("/users/" + learnerUser.getId()).session(leaderSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Public Reading Stats")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(learnerUser.getUsername())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Geometry Guild")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Edit Profile"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Delete account"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("action=\"/profile\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("type=\"password\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("learner-3@example.com")
                )));
    }

    @Test
    void unauthenticatedUserCannotViewPublicProfileRouteByPolicy() throws Exception {
        AuthUser viewer = authRepository.save(new AuthUser(
                "viewer",
                "viewer@example.com",
                null,
                "viewer",
                passwordEncoder.encode("ViewerPass1!"),
                AuthRole.USER
        ));

        mockMvc.perform(get("/users/" + viewer.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void leaderCanDeleteClan() throws Exception {
        MockHttpSession leaderSession = loginAs("delete-leader@example.com", "LeaderPass1!");

        mockMvc.perform(post("/clans")
                        .session(leaderSession)
                        .with(csrf())
                        .param("name", "Disposable Guild"))
                .andExpect(status().is3xxRedirection());

        var clan = clanRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/clans/" + clan.getId() + "/delete")
                        .session(leaderSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans"));

        assertThat(clanRepository.count()).isEqualTo(0);
    }

    @Test
    void nonLeaderCannotDeleteClan() throws Exception {
        MockHttpSession leaderSession = loginAs("owner@example.com", "LeaderPass1!");
        MockHttpSession memberSession = loginAs("member@example.com", "MemberPass1!");

        mockMvc.perform(post("/clans")
                        .session(leaderSession)
                        .with(csrf())
                        .param("name", "Protected Guild"))
                .andExpect(status().is3xxRedirection());

        var clan = clanRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/clans/" + clan.getId() + "/join")
                        .session(memberSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        var joinRequest = clanJoinRequestRepository.findAll().getFirst();
        mockMvc.perform(post("/clans/" + clan.getId() + "/requests/" + joinRequest.getId() + "/decision")
                        .session(leaderSession)
                        .with(csrf())
                        .param("action", "approve"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/clans/" + clan.getId() + "/delete")
                        .session(memberSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clans/" + clan.getId()));

        assertThat(clanRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCanEndSeasonAndPromoteTopBronzeClan() throws Exception {
        MockHttpSession adminSession = loginAs("admin@example.com", "AdminPass1!", AuthRole.ADMIN);
        MockHttpSession alphaSession = loginAs("alpha@example.com", "AlphaPass1!");
        MockHttpSession betaSession = loginAs("beta@example.com", "BetaPass1!");
        MockHttpSession gammaSession = loginAs("gamma@example.com", "GammaPass1!");
        MockHttpSession deltaSession = loginAs("delta@example.com", "DeltaPass1!");

        createClan(alphaSession, "Alpha Clan");
        createClan(betaSession, "Beta Clan");
        createClan(gammaSession, "Gamma Clan");
        createClan(deltaSession, "Delta Clan");

        postClanQuizScore(alphaSession, "alpha@example.com", 95.0d, 0.95d);
        postClanQuizScore(betaSession, "beta@example.com", 75.0d, 0.75d);
        postClanQuizScore(gammaSession, "gamma@example.com", 55.0d, 0.55d);
        postClanQuizScore(deltaSession, "delta@example.com", 35.0d, 0.60d);

        mockMvc.perform(post("/admin/league/season/end")
                        .session(adminSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leaderboard?tier=BRONZE"));

        var alphaClan = clanRepository.findAllForListing().stream()
                .filter(clan -> clan.getName().equals("Alpha Clan"))
                .findFirst()
                .orElseThrow();
        assertThat(alphaClan.getTier().getCode()).isEqualTo(TierCode.SILVER);
    }

    @Test
    void nonAdminCannotEndSeason() throws Exception {
        MockHttpSession userSession = loginAs("regular@example.com", "RegularPass1!");

        mockMvc.perform(post("/admin/league/season/end")
                        .session(userSession)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession loginAs(String email, String rawPassword) throws Exception {
        return loginAs(email, rawPassword, AuthRole.USER);
    }

    private MockHttpSession loginAs(String email, String rawPassword, AuthRole role) throws Exception {
        authRepository.save(new AuthUser(
                "user-" + email.hashCode(),
                email,
                null,
                "user",
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

    private void createClan(MockHttpSession session, String clanName) throws Exception {
        mockMvc.perform(post("/clans")
                        .session(session)
                        .with(csrf())
                        .param("name", clanName))
                .andExpect(status().is3xxRedirection());
    }

    private void postClanQuizScore(
            MockHttpSession session,
            String memberEmail,
            double score,
            double accuracy
    ) throws Exception {
        var member = authRepository.findByEmail(memberEmail).orElseThrow();
        mockMvc.perform(post("/api/league/events/quiz-completions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"%s",
                                  "userId":"%s",
                                  "textId":"%s",
                                  "score":%s,
                                  "accuracy":%s,
                                  "completedAt":"%s"
                                }
                                """.formatted(
                                UUID.randomUUID(),
                                member.getId(),
                                UUID.randomUUID(),
                                score,
                                accuracy,
                                LocalDateTime.now().minusMinutes(1)
                        )))
                .andExpect(status().isAccepted());
    }
}
