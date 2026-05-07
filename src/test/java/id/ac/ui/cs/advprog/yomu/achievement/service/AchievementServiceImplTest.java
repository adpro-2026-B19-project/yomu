package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.repository.AchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserStatisticRepository;
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

    @Mock
    private UserStatisticRepository userStatisticRepository;

    @InjectMocks
    private AchievementServiceImpl achievementService;

    private Achievement sampleAchievement;

    @BeforeEach
    void setUp() {
        sampleAchievement = Achievement.builder()
                .id(1L)
                .name("First Steps")
                .milestone("Complete your first reading")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(1)
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

        Achievement result = achievementService.createAchievement(
                "First Steps",
                "Complete your first reading",
                AchievementRequirementType.READING_COUNT,
                1
        );

        assertThat(result.getName()).isEqualTo("First Steps");
        assertThat(result.getMilestone()).isEqualTo("Complete your first reading");
        assertThat(result.getRequirementType()).isEqualTo(AchievementRequirementType.READING_COUNT);
        assertThat(result.getTargetValue()).isEqualTo(1);
        verify(achievementRepository).save(any(Achievement.class));
    }

    @Test
    void createAchievement_throwsWhenNameAlreadyExists() {
        when(achievementRepository.existsByName("First Steps")).thenReturn(true);

        assertThatThrownBy(() -> achievementService.createAchievement(
                "First Steps",
                "some milestone",
                AchievementRequirementType.READING_COUNT,
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sudah ada");

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

    @Test
    void processQuizCompletion_unlocksEligibleAchievements() {
        UUID userId = UUID.randomUUID();
        when(achievementRepository.findAll()).thenReturn(List.of(
                Achievement.builder().id(1L).name("First Steps").milestone("Complete 1 reading").requirementType(AchievementRequirementType.READING_COUNT).targetValue(1).build(),
                Achievement.builder().id(2L).name("Reader").milestone("Complete 3 readings").requirementType(AchievementRequirementType.READING_COUNT).targetValue(3).build(),
                Achievement.builder().id(3L).name("Veteran").milestone("Complete 5 readings").requirementType(AchievementRequirementType.READING_COUNT).targetValue(5).build()
        ));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());

        achievementService.processQuizCompletion(userId, LocalDateTime.now());

        verify(userStatisticRepository).save(any());
        verify(userAchievementRepository, times(1)).save(any(UserAchievement.class));
    }
}
