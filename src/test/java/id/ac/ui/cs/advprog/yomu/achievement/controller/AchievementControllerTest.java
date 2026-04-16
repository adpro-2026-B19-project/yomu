package id.ac.ui.cs.advprog.yomu.achievement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.achievement.dto.AchievementCreateForm;
import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.service.AchievementService;
import java.util.List;
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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void achievementListPageShouldSetAchievementsAndLoggedInNameWhenAuthenticated() {
        AchievementController controller = new AchievementController(achievementService);
        Achievement achievement = Achievement.builder().name("First Win").milestone("M1").build();
        when(achievementService.getAllAchievements()).thenReturn(List.of(achievement));

        Authentication authenticated = new TestingAuthenticationToken("player", "pass", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(authenticated);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.achievementListPage(model);

        assertThat(view).isEqualTo("achievement/ListAchievement");
        assertThat(model.getAttribute("achievements")).isEqualTo(List.of(achievement));
        assertThat(model.getAttribute("loggedInName")).isEqualTo("player");
    }

    @Test
    void achievementListPageShouldNotSetLoggedInNameForAnonymous() {
        AchievementController controller = new AchievementController(achievementService);
        when(achievementService.getAllAchievements()).thenReturn(List.of());
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
        AchievementController controller = new AchievementController(achievementService);
        UUID userId = UUID.randomUUID();
        Achievement achievement = Achievement.builder().name("A").milestone("M").build();
        UserAchievement userAchievement = new UserAchievement();
        AchievementCreateForm form = new AchievementCreateForm("A", "M");

        when(achievementService.getAllAchievements()).thenReturn(List.of(achievement));
        when(achievementService.getAchievementsByUserId(userId)).thenReturn(List.of(userAchievement));
        when(achievementService.createAchievement("A", "M")).thenReturn(achievement);

        assertThat(controller.getAllAchievements().getBody()).containsExactly(achievement);
        assertThat(controller.getAchievementsByUser(userId).getBody()).containsExactly(userAchievement);
        assertThat(controller.createAchievement(form).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.handleDuplicate(new IllegalArgumentException("dup")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
