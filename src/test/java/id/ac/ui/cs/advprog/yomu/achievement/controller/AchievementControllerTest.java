package id.ac.ui.cs.advprog.yomu.achievement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.achievement.dto.AchievementCreateForm;
import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;
import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import id.ac.ui.cs.advprog.yomu.achievement.service.DailyMissionService;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import java.time.LocalDate;
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

    @Test
    void achievementListPageShouldPopulateAchievementCardViewWhenUserHasProgress() {
        // Tests AchievementCardView creation
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        
        Category category = new Category("Technology");
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        
        Achievement achievement = Achievement.builder()
                .id(1L)
                .name("First Reader")
                .milestone("Complete 1 reading")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(3)
                .build();
        when(achievementService.getAllAchievements()).thenReturn(List.of(achievement));
        
        AuthUser authUser = new AuthUser("testuser");
        setUserId(authUser);
        
        AchievementService.AchievementProgress progress = new AchievementService.AchievementProgress(
                1L,
                "First Reader",
                "Complete 1 reading",
                AchievementRequirementType.READING_COUNT,
                3,
                1.0d,
                33,
                false,
                false
        );
        
        when(achievementService.getAchievementProgress(authUser.getId())).thenReturn(List.of(progress));
        when(dailyMissionService.getTodayMissions()).thenReturn(List.of());
        when(dailyMissionService.getUserProgress(authUser.getId())).thenReturn(List.of());
        
        Authentication auth = new TestingAuthenticationToken("testuser", "pass", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(currentUserResolver.resolveUser(auth)).thenReturn(Optional.of(authUser));
        
        ExtendedModelMap model = new ExtendedModelMap();
        controller.achievementListPage(model);
        
        @SuppressWarnings("unchecked")
        List<Object> achievementCards = (List<Object>) model.getAttribute("achievementProgressCards");
        assertThat(achievementCards).hasSize(1);
    }

    @Test
    void achievementListPageShouldPopulateDailyMissionCardViewWhenUserHasMissions() {
        // Tests DailyMissionCardView creation
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        
        Category category = new Category("Technology");
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(achievementService.getAllAchievements()).thenReturn(List.of());
        
        AuthUser authUser = new AuthUser("testuser");
        setUserId(authUser);
        
        DailyMission mission = DailyMission.builder()
                .id(1L)
                .title("Read 3 articles")
                .targetCount(3)
                .activeDate(LocalDate.now())
                .primary(true)
                .categoryId(1L)
                .build();
        
        UserMissionProgress missionProgress = UserMissionProgress.builder()
                .id(1L)
                .userId(authUser.getId())
                .mission(mission)
                .currentProgress(1)
                .completed(false)
                .build();
        
        when(dailyMissionService.getTodayMissions()).thenReturn(List.of(mission));
        when(dailyMissionService.getUserProgress(authUser.getId())).thenReturn(List.of(missionProgress));
        when(achievementService.getAchievementProgress(authUser.getId())).thenReturn(List.of());
        
        Authentication auth = new TestingAuthenticationToken("testuser", "pass", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(currentUserResolver.resolveUser(auth)).thenReturn(Optional.of(authUser));
        
        ExtendedModelMap model = new ExtendedModelMap();
        controller.achievementListPage(model);
        
        @SuppressWarnings("unchecked")
        List<Object> missionCards = (List<Object>) model.getAttribute("todayMissionCards");
        assertThat(missionCards).hasSize(1);
    }

    @Test
    void achievementListPageShouldShowCompletedStatusWhenMissionIsCompleted() {
        // Tests DailyMissionCardView with completed status
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(achievementService.getAllAchievements()).thenReturn(List.of());
        
        AuthUser authUser = new AuthUser("testuser");
        setUserId(authUser);
        
        DailyMission mission = DailyMission.builder()
                .id(1L)
                .title("Complete mission")
                .targetCount(2)
                .activeDate(LocalDate.now())
                .primary(false)
                .categoryId(null)
                .build();
        
        UserMissionProgress completedProgress = UserMissionProgress.builder()
                .id(1L)
                .userId(authUser.getId())
                .mission(mission)
                .currentProgress(2)
                .completed(true)
                .build();
        
        when(dailyMissionService.getTodayMissions()).thenReturn(List.of(mission));
        when(dailyMissionService.getUserProgress(authUser.getId())).thenReturn(List.of(completedProgress));
        when(achievementService.getAchievementProgress(authUser.getId())).thenReturn(List.of());
        
        Authentication auth = new TestingAuthenticationToken("testuser", "pass", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(currentUserResolver.resolveUser(auth)).thenReturn(Optional.of(authUser));
        
        ExtendedModelMap model = new ExtendedModelMap();
        controller.achievementListPage(model);
        
        @SuppressWarnings("unchecked")
        List<Object> missionCards = (List<Object>) model.getAttribute("todayMissionCards");
        assertThat(missionCards).hasSize(1);
    }

    @Test
    void achievementListPageShouldShowUnlockedAchievementStatus() {
        // Tests AchievementCardView with unlocked status
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(achievementService.getAllAchievements()).thenReturn(List.of());
        
        AuthUser authUser = new AuthUser("testuser");
        setUserId(authUser);
        
        AchievementService.AchievementProgress unlockedProgress = new AchievementService.AchievementProgress(
                1L,
                "Achievement Name",
                "Milestone description",
                AchievementRequirementType.TOTAL_SCORE,
                100,
                120.5d,
                100,
                true,
                true
        );
        
        when(achievementService.getAchievementProgress(authUser.getId())).thenReturn(List.of(unlockedProgress));
        when(dailyMissionService.getTodayMissions()).thenReturn(List.of());
        when(dailyMissionService.getUserProgress(authUser.getId())).thenReturn(List.of());
        
        Authentication auth = new TestingAuthenticationToken("testuser", "pass", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(currentUserResolver.resolveUser(auth)).thenReturn(Optional.of(authUser));
        
        ExtendedModelMap model = new ExtendedModelMap();
        controller.achievementListPage(model);
        
        @SuppressWarnings("unchecked")
        List<Object> achievementCards = (List<Object>) model.getAttribute("achievementProgressCards");
        assertThat(achievementCards).hasSize(1);
    }

    @Test
    void achievementListPageShouldShowAdminViewWithDistribution() {
        AchievementController controller = new AchievementController(achievementService, dailyMissionService, currentUserResolver, categoryRepository);
        
        when(achievementService.getAllAchievements()).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        
        AchievementService.AchievementDistribution distribution = 
            new AchievementService.AchievementDistribution(1L, "Test Achievement", "Milestone", 5L);
        when(achievementService.getAchievementDistribution()).thenReturn(List.of(distribution));
        
        Authentication auth = new TestingAuthenticationToken("admin", "pass", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        ExtendedModelMap model = new ExtendedModelMap();
        controller.achievementListPage(model);
        
        assertThat(model.getAttribute("adminView")).isEqualTo(true);
        assertThat(model.getAttribute("achievementDistribution")).asList().hasSize(1);
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
