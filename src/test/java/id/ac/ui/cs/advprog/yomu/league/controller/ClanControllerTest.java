package id.ac.ui.cs.advprog.yomu.league.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.league.dto.ClanCreateForm;
import id.ac.ui.cs.advprog.yomu.league.service.ClanService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class ClanControllerTest {

    @Mock
    private ClanService clanService;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Mock
    private AuthRepository authRepository;

    private ClanController controller;

    private final Authentication userAuthentication =
            new UsernamePasswordAuthenticationToken("user", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    private final Authentication adminAuthentication =
            new UsernamePasswordAuthenticationToken("admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    @BeforeEach
    void setUp() {
        controller = new ClanController(clanService, currentUserResolver, authRepository);
    }

    @Test
    void clanListPageShouldRenderAndPopulateModel() {
        UUID creatorId = UUID.randomUUID();
        AuthUser creator = createUser(creatorId, "creator", "Creator", AuthRole.USER);
        when(clanService.listClans()).thenReturn(List.of(
                new ClanService.ClanSummary(UUID.randomUUID(), "Clan One", "BRONZE", 1L, creatorId)
        ));
        when(authRepository.findAllById(any())).thenReturn(List.of(creator));
        when(currentUserResolver.resolveUsername(userAuthentication)).thenReturn(Optional.of("viewer"));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.clanListPage(model, userAuthentication);

        assertThat(view).isEqualTo("league/clans");
        assertThat(model.getAttribute("clans")).isNotNull();
        assertThat(model.getAttribute("loggedInName")).isEqualTo("viewer");
        assertThat(model.getAttribute("createForm")).isNotNull();
    }

    @Test
    void leaderboardPageShouldRenderSelectedTier() {
        UUID clanId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        when(clanService.getLeaderboard(id.ac.ui.cs.advprog.yomu.league.model.TierCode.SILVER)).thenReturn(List.of(
                new ClanService.LeaderboardEntry(
                        clanId,
                        "Clan One",
                        "SILVER",
                        3L,
                        10.0d,
                        12.0d,
                        List.of(new ClanService.ScoreModifier("PRODUCTIVITY_BUFF", "Productivity Buff", 1.2d, "desc")),
                        "Silver formula"
                )
        ));
        when(clanService.listClans()).thenReturn(List.of(
                new ClanService.ClanSummary(clanId, "Clan One", "SILVER", 3L, creatorId)
        ));
        when(authRepository.findAllById(any())).thenReturn(List.of(createUser(creatorId, "creator", "Creator", AuthRole.USER)));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.leaderboardPage("silver", model, adminAuthentication);

        assertThat(view).isEqualTo("league/leaderboard");
        assertThat(model.getAttribute("entries")).isNotNull();
        assertThat(model.getAttribute("selectedTier")).isEqualTo("SILVER");
        assertThat(model.getAttribute("adminCanEndSeason")).isEqualTo(true);
    }

    @Test
    void clanDetailPageShouldRedirectWhenNotFound() {
        UUID clanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AuthUser currentUser = createUser(userId, "viewer", "Viewer", AuthRole.USER);

        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(currentUser));
        when(clanService.getClanDetail(clanId, userId)).thenThrow(new IllegalArgumentException("Clan was not found"));

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String view = controller.clanDetailPage(clanId, new ExtendedModelMap(), flash, userAuthentication);

        assertThat(view).isEqualTo("redirect:/clans");
        assertThat(flash.getFlashAttributes().get("error")).isEqualTo("Clan was not found");
    }

    @Test
    void clanDetailPageShouldRenderWhenFound() {
        UUID clanId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        AuthUser viewer = createUser(viewerId, "viewer", "Viewer", AuthRole.USER);

        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(viewer));
        when(currentUserResolver.resolveUsername(userAuthentication)).thenReturn(Optional.of("viewer"));
        when(clanService.getClanDetail(clanId, viewerId)).thenReturn(new ClanService.ClanDetail(
                clanId,
                "Clan One",
                "BRONZE",
                2L,
                creatorId,
                true,
                true,
                false,
                List.of(new ClanService.ClanMemberSummary(memberId, "MEMBER")),
                List.of(new ClanService.JoinRequestSummary(UUID.randomUUID(), requesterId, LocalDateTime.now()))
        ));
        when(authRepository.findAllById(any())).thenReturn(List.of(
                createUser(creatorId, "creator", "Creator", AuthRole.USER),
                createUser(memberId, "member", "Member", AuthRole.USER),
                createUser(requesterId, "requester", "Requester", AuthRole.USER)
        ));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.clanDetailPage(clanId, model, new RedirectAttributesModelMap(), userAuthentication);

        assertThat(view).isEqualTo("league/clan-detail");
        assertThat(model.getAttribute("clan")).isNotNull();
    }

    @Test
    void createClanShouldRedirectToLoginWhenUnauthenticated() {
        ClanCreateForm form = new ClanCreateForm("Alpha");
        BindingResult result = new BeanPropertyBindingResult(form, "createForm");
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.empty());

        String view = controller.createClan(form, result, new RedirectAttributesModelMap(), userAuthentication);

        assertThat(view).isEqualTo("redirect:/auth/login");
        verify(clanService, never()).createClan(any(), any());
    }

    @Test
    void createClanShouldRedirectToClansWhenBindingFails() {
        UUID userId = UUID.randomUUID();
        AuthUser currentUser = createUser(userId, "viewer", "Viewer", AuthRole.USER);
        ClanCreateForm form = new ClanCreateForm("");
        BindingResult result = new BeanPropertyBindingResult(form, "createForm");
        result.rejectValue("name", "required", "required");
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(currentUser));

        String view = controller.createClan(form, result, new RedirectAttributesModelMap(), userAuthentication);

        assertThat(view).isEqualTo("redirect:/clans");
    }

    @Test
    void createClanShouldHandleServiceValidationError() {
        UUID userId = UUID.randomUUID();
        AuthUser currentUser = createUser(userId, "viewer", "Viewer", AuthRole.USER);
        ClanCreateForm form = new ClanCreateForm("Alpha");
        BindingResult result = new BeanPropertyBindingResult(form, "createForm");
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(currentUser));
        when(clanService.createClan(any(), any())).thenThrow(new IllegalArgumentException("Clan name already exists"));

        String view = controller.createClan(form, result, new RedirectAttributesModelMap(), userAuthentication);

        assertThat(view).isEqualTo("redirect:/clans");
        assertThat(result.hasFieldErrors("name")).isTrue();
    }

    @Test
    void createClanShouldRedirectWithSuccessWhenValid() {
        UUID userId = UUID.randomUUID();
        AuthUser currentUser = createUser(userId, "viewer", "Viewer", AuthRole.USER);
        ClanCreateForm form = new ClanCreateForm("Alpha");
        BindingResult result = new BeanPropertyBindingResult(form, "createForm");
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(currentUser));
        when(clanService.createClan(any(), any())).thenReturn(
                new ClanService.ClanSummary(UUID.randomUUID(), "Alpha", "BRONZE", 1L, userId)
        );

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String view = controller.createClan(form, result, flash, userAuthentication);

        assertThat(view).isEqualTo("redirect:/clans");
        assertThat(flash.getFlashAttributes().get("success")).isEqualTo("Clan created successfully");
    }

    @Test
    void submitJoinRequestShouldHandleSuccessAndValidationError() {
        UUID userId = UUID.randomUUID();
        UUID clanId = UUID.randomUUID();
        AuthUser currentUser = createUser(userId, "viewer", "Viewer", AuthRole.USER);
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(currentUser));

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String successView = controller.submitJoinRequest(clanId, flash, userAuthentication);
        assertThat(successView).isEqualTo("redirect:/clans/" + clanId);
        assertThat(flash.getFlashAttributes().get("success")).isEqualTo("Join request has been sent");

        doThrow(new IllegalArgumentException("already pending"))
                .when(clanService).submitJoinRequest(clanId, userId);
        RedirectAttributesModelMap flashError = new RedirectAttributesModelMap();
        String errorView = controller.submitJoinRequest(clanId, flashError, userAuthentication);
        assertThat(errorView).isEqualTo("redirect:/clans/" + clanId);
        assertThat(flashError.getFlashAttributes().get("error")).isEqualTo("already pending");
    }

    @Test
    void reviewJoinRequestShouldHandleUnknownAction() {
        UUID userId = UUID.randomUUID();
        UUID clanId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        AuthUser currentUser = createUser(userId, "viewer", "Viewer", AuthRole.USER);
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(currentUser));

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String view = controller.reviewJoinRequest(clanId, requestId, "invalid", flash, userAuthentication);

        assertThat(view).isEqualTo("redirect:/clans/" + clanId);
        assertThat(flash.getFlashAttributes().get("error")).isEqualTo("Unknown action");
    }

    @Test
    void deleteClanShouldHandleSuccessAndValidationError() {
        UUID userId = UUID.randomUUID();
        UUID clanId = UUID.randomUUID();
        AuthUser currentUser = createUser(userId, "viewer", "Viewer", AuthRole.USER);
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.of(currentUser));

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String successView = controller.deleteClan(clanId, flash, userAuthentication);
        assertThat(successView).isEqualTo("redirect:/clans");
        assertThat(flash.getFlashAttributes().get("success")).isEqualTo("Clan deleted successfully");

        doThrow(new IllegalArgumentException("Only clan leader can delete this clan"))
                .when(clanService).deleteClan(clanId, userId);
        RedirectAttributesModelMap flashError = new RedirectAttributesModelMap();
        String errorView = controller.deleteClan(clanId, flashError, userAuthentication);
        assertThat(errorView).isEqualTo("redirect:/clans/" + clanId);
        assertThat(flashError.getFlashAttributes().get("error")).isEqualTo("Only clan leader can delete this clan");
    }

    @Test
    void publicProfilePageShouldRedirectWhenServiceThrows() {
        UUID userId = UUID.randomUUID();
        when(clanService.getPublicProfile(userId)).thenThrow(new IllegalArgumentException("User was not found"));

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String view = controller.publicProfilePage(userId, new ExtendedModelMap(), flash, userAuthentication);

        assertThat(view).isEqualTo("redirect:/clans");
        assertThat(flash.getFlashAttributes().get("error")).isEqualTo("User was not found");
    }

    @Test
    void publicProfilePageShouldRender() {
        UUID userId = UUID.randomUUID();
        when(clanService.getPublicProfile(userId)).thenReturn(new ClanService.PublicProfile(
                userId,
                "player",
                "Player",
                "USER",
                "Clan One",
                "BRONZE",
                "MEMBER",
                12.0d,
                2L,
                8.0d,
                0.8d,
                List.of(new ClanService.DisplayedAchievement(1L, "Pinned", "Read 2 texts", LocalDateTime.now()))
        ));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.publicProfilePage(userId, model, new RedirectAttributesModelMap(), userAuthentication);

        assertThat(view).isEqualTo("league/public-profile");
        assertThat(model.getAttribute("publicProfile")).isNotNull();
    }

    @Test
    void endSeasonShouldRedirectWithSummaryMessage() {
        when(clanService.endCurrentSeason()).thenReturn(new ClanService.SeasonTransitionResult(
                1,
                2,
                List.of(new ClanService.TierChange(UUID.randomUUID(), "Clan One", "BRONZE", "SILVER", "PROMOTION"))
        ));

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String view = controller.endSeason(flash);

        assertThat(view).isEqualTo("redirect:/leaderboard?tier=BRONZE");
        assertThat(flash.getFlashAttributes().get("success").toString()).contains("Season 1 ended");
    }

    @Test
    void submitJoinRequestShouldRedirectLoginWhenUnauthenticated() {
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.empty());
        String view = controller.submitJoinRequest(UUID.randomUUID(), new RedirectAttributesModelMap(), userAuthentication);
        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    @Test
    void reviewJoinRequestShouldRedirectLoginWhenUnauthenticated() {
        when(currentUserResolver.resolveUser(userAuthentication)).thenReturn(Optional.empty());
        String view = controller.reviewJoinRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "approve",
                new RedirectAttributesModelMap(),
                userAuthentication
        );
        assertThat(view).isEqualTo("redirect:/auth/login");
    }

    private AuthUser createUser(UUID id, String username, String displayName, AuthRole role) {
        AuthUser user = new AuthUser(username, username + "@example.com", null, displayName, "encoded", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
