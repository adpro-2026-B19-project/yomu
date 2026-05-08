package id.ac.ui.cs.advprog.yomu.achievement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.achievement.dto.AchievementCreateForm;
import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import id.ac.ui.cs.advprog.yomu.achievement.service.DailyMissionService;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;

@ExtendWith(MockitoExtension.class)
class AchievementControllerTest {

    @Mock
    private AchievementService achievementService;

    @Mock
    private DailyMissionService dailyMissionService;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Mock
    private CategoryRepository categoryRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void achievementListPageShouldSetAchievementsAndLoggedInNameWhenAuthenticated() {
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        Achievement achievement = Achievement.builder()
                .name("First Win")
                .milestone("M1")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(1)
                .build();
        when(achievementService.getAllAchievements()).thenReturn(List.of(achievement));
        when(categoryRepository.findAll()).thenReturn(List.of());
        AuthUser authUser = new AuthUser("user1");
        setUserId(authUser);

        Authentication auth = new TestingAuthenticationToken("user1", "pass", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(currentUserResolver.resolveUser(auth)).thenReturn(Optional.of(authUser));
        when(achievementService.getAchievementProgress(authUser.getId())).thenReturn(List.of());
        when(dailyMissionService.getTodayMissions()).thenReturn(List.of());
        when(dailyMissionService.getUserProgress(authUser.getId())).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();
        String viewName = controller.achievementListPage(model);

        assertThat(viewName).isEqualTo("achievement/ListAchievement");
        assertThat(model.getAttribute("achievements")).asList().containsExactly(achievement);
        assertThat(model.getAttribute("loggedInName")).isEqualTo("user1");
    }

    @Test
    void achievementListPageShouldNotSetLoggedInNameWhenNotAuthenticated() {
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        when(achievementService.getAllAchievements()).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        SecurityContextHolder.getContext().setAuthentication(null);

        ExtendedModelMap model = new ExtendedModelMap();
        controller.achievementListPage(model);

        assertThat(model.containsAttribute("loggedInName")).isFalse();
    }

    @Test
    void achievementListPageShouldNotSetLoggedInNameForAnonymousUser() {
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        when(achievementService.getAllAchievements()).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", List.of(
                        new SimpleGrantedAuthority("ROLE_ANONYMOUS")
                ))
        );

        ExtendedModelMap model = new ExtendedModelMap();
        controller.achievementListPage(model);

        assertThat(model.containsAttribute("loggedInName")).isFalse();
    }

    @Test
    void restEndpointsShouldReturnExpectedResponse() {
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        UUID userId = UUID.randomUUID();
        Achievement achievement = Achievement.builder()
                .name("A")
                .milestone("M")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(1)
                .build();
        UserAchievement userAchievement = new UserAchievement();
        AchievementCreateForm form = new AchievementCreateForm(
                "A",
                "M",
                AchievementRequirementType.READING_COUNT,
                1
        );

        when(achievementService.getAllAchievements()).thenReturn(List.of(achievement));
        when(achievementService.getAchievementsByUserId(userId)).thenReturn(List.of(userAchievement));
        when(achievementService.createAchievement("A", "M", AchievementRequirementType.READING_COUNT, 1)).thenReturn(achievement);
        when(achievementService.updateAchievement(1L, "A", "M", AchievementRequirementType.READING_COUNT, 1)).thenReturn(achievement);

        assertThat(controller.getAllAchievements().getBody()).containsExactly(achievement);
        assertThat(controller.getAchievementsByUser(userId).getBody()).containsExactly(userAchievement);
        assertThat(controller.createAchievement(form).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateAchievement(1L, form).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteAchievement(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.handleDuplicate(new IllegalArgumentException("dup")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    private void setUserId(AuthUser authUser) {
        try {
            java.lang.reflect.Field idField = AuthUser.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(authUser, UUID.randomUUID());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
