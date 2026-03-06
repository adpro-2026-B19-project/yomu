package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.repository.AchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceImplTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @InjectMocks
    private AchievementServiceImpl achievementService;

    private Achievement sampleAchievement;

    @BeforeEach
    void setUp() {
        sampleAchievement = Achievement.builder()
                .id(1L)
                .name("First Steps")
                .milestone("Complete your first reading")
                .build();
    }

    @Test
    void getAllAchievements_returnsAllAchievements() {
        when(achievementRepository.findAll()).thenReturn(List.of(sampleAchievement));

        List<Achievement> result = achievementService.getAllAchievements();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("First Steps");
        verify(achievementRepository).findAll();
    }

    @Test
    void getAllAchievements_returnsEmptyListWhenNoneExist() {
        when(achievementRepository.findAll()).thenReturn(List.of());

        List<Achievement> result = achievementService.getAllAchievements();

        assertThat(result).isEmpty();
    }

    @Test
    void createAchievement_savesAndReturnsAchievement() {
        when(achievementRepository.existsByName("First Steps")).thenReturn(false);
        when(achievementRepository.save(any(Achievement.class))).thenReturn(sampleAchievement);

        Achievement result = achievementService.createAchievement("First Steps", "Complete your first reading");

        assertThat(result.getName()).isEqualTo("First Steps");
        assertThat(result.getMilestone()).isEqualTo("Complete your first reading");
        verify(achievementRepository).save(any(Achievement.class));
    }

    @Test
    void createAchievement_throwsWhenNameAlreadyExists() {
        when(achievementRepository.existsByName("First Steps")).thenReturn(true);

        assertThatThrownBy(() -> achievementService.createAchievement("First Steps", "some milestone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(achievementRepository, never()).save(any());
    }

    @Test
    void getAchievementsByUserId_returnsUserAchievements() {
        UUID userId = UUID.randomUUID();
        UserAchievement ua = UserAchievement.builder()
                .id(1L)
                .userId(userId)
                .achievement(sampleAchievement)
                .unlockedAt(LocalDateTime.now())
                .build();

        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(ua));

        List<UserAchievement> result = achievementService.getAchievementsByUserId(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        verify(userAchievementRepository).findByUserId(userId);
    }

    @Test
    void getAchievementsByUserId_returnsEmptyListForUserWithNoAchievements() {
        UUID userId = UUID.randomUUID();
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());

        List<UserAchievement> result = achievementService.getAchievementsByUserId(userId);

        assertThat(result).isEmpty();
    }
}