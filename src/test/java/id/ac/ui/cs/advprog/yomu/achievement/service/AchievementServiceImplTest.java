package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserStatistic;
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
    void getAchievementDistribution_returnsUnlockCountsForEachAchievement() {
        Achievement secondAchievement = Achievement.builder()
                .id(2L)
                .name("Reader")
                .milestone("Complete 3 readings")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(3)
                .build();
        when(achievementRepository.findAll()).thenReturn(List.of(sampleAchievement, secondAchievement));
        when(userAchievementRepository.countByAchievement_Id(1L)).thenReturn(5L);
        when(userAchievementRepository.countByAchievement_Id(2L)).thenReturn(2L);

        List<AchievementService.AchievementDistribution> result = achievementService.getAchievementDistribution();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).achievementName()).isEqualTo("First Steps");
        assertThat(result.get(0).unlockedUserCount()).isEqualTo(5L);
        assertThat(result.get(1).achievementName()).isEqualTo("Reader");
        assertThat(result.get(1).unlockedUserCount()).isEqualTo(2L);
    }

    @Test
    void getAchievementProgress_returnsProgressForReadingAndScoreBasedAchievements() {
        UUID userId = UUID.randomUUID();
        UserStatistic statistic = UserStatistic.builder()
                .userId(userId)
                .totalReadings(2)
                .totalScore(150.0d)
                .build();
        Achievement readingAchievement = Achievement.builder()
                .id(1L)
                .name("Reader")
                .milestone("Complete 3 readings")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(3)
                .build();
        Achievement scoreAchievement = Achievement.builder()
                .id(2L)
                .name("Scorer")
                .milestone("Reach 200 total score")
                .requirementType(AchievementRequirementType.TOTAL_SCORE)
                .targetValue(200)
                .build();
        when(userStatisticRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(statistic));
        when(achievementRepository.findAll()).thenReturn(List.of(readingAchievement, scoreAchievement));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());

        List<AchievementService.AchievementProgress> progress = achievementService.getAchievementProgress(userId);

        assertThat(progress).hasSize(2);
        assertThat(progress.get(0).currentValue()).isEqualTo(2.0d);
        assertThat(progress.get(0).progressPercent()).isEqualTo(66);
        assertThat(progress.get(1).currentValue()).isEqualTo(150.0d);
        assertThat(progress.get(1).progressPercent()).isEqualTo(75);
    }

    @Test
    void processQuizCompletion_unlocksEligibleAchievements() {
        UUID userId = UUID.randomUUID();
        when(achievementRepository.findAll()).thenReturn(List.of(
                Achievement.builder().id(1L).name("First Steps").milestone("Complete 1 reading").requirementType(AchievementRequirementType.READING_COUNT).targetValue(1).build(),
                Achievement.builder().id(2L).name("Score Hunter").milestone("Reach 100 total score").requirementType(AchievementRequirementType.TOTAL_SCORE).targetValue(100).build(),
                Achievement.builder().id(3L).name("Veteran").milestone("Complete 5 readings").requirementType(AchievementRequirementType.READING_COUNT).targetValue(5).build()
        ));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, 1L)).thenReturn(false);
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, 2L)).thenReturn(false);

        achievementService.processQuizCompletion(userId, 100.0d, LocalDateTime.now());

        verify(userStatisticRepository).save(any());
        verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
    }

    @Test
    void processQuizCompletion_implementsIdempotency_achievementNotUnlockedTwice() {
        // MILESTONE 100% DELIVERABLE: Idempotency
        // Achievement tidak dapat terbuka dua kali untuk satu pelajar yang sama
        UUID userId = UUID.randomUUID();
        Achievement testAchievement = Achievement.builder()
                .id(1L)
                .name("First Reader")
                .milestone("Complete 1 reading")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(1)
                .build();

        // Setup: Simulate achievement already unlocked
        UserAchievement alreadyUnlockedAchievement = UserAchievement.builder()
                .id(1L)
                .userId(userId)
                .achievement(testAchievement)
                .displayed(false)
                .unlockedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userStatisticRepository.findByUserId(userId))
                .thenReturn(java.util.Optional.of(UserStatistic.builder()
                        .userId(userId)
                        .totalReadings(0)
                        .totalScore(0.0d)
                        .build()));
        when(achievementRepository.findAll()).thenReturn(List.of(testAchievement));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(alreadyUnlockedAchievement));

        // Act: Process quiz completion again
        achievementService.processQuizCompletion(userId, 100.0d, LocalDateTime.now());

        // Assert: Verify save was NOT called for the already-unlocked achievement
        // The unobtainedAchievements filter should exclude it, and existsByUserIdAndAchievementId check should prevent duplicate save
        verify(userAchievementRepository, never()).save(argThat(ua -> ua.getAchievement().getId().equals(1L)));
    }

    @Test
    void updateAchievement_successfullyUpdatesExistingAchievement() {
        Long achievementId = 1L;
        when(achievementRepository.findById(achievementId)).thenReturn(java.util.Optional.of(sampleAchievement));
        when(achievementRepository.findByName("Updated Name")).thenReturn(java.util.Optional.empty());
        when(achievementRepository.save(any(Achievement.class))).thenReturn(sampleAchievement);

        Achievement result = achievementService.updateAchievement(
                achievementId,
                "Updated Name",
                "Updated milestone",
                AchievementRequirementType.TOTAL_SCORE,
                10
        );

        assertThat(result).isNotNull();
        verify(achievementRepository).findById(achievementId);
        verify(achievementRepository).save(any(Achievement.class));
    }

    @Test
    void updateAchievement_throwsWhenAchievementNotFound() {
        Long nonExistentId = 999L;
        when(achievementRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> achievementService.updateAchievement(
                nonExistentId,
                "Name",
                "Milestone",
                AchievementRequirementType.READING_COUNT,
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Achievement tidak ditemukan");

        verify(achievementRepository, never()).save(any());
    }

    @Test
    void updateAchievement_throwsWhenNameAlreadyExistsForOtherAchievement() {
        Long achievementId = 1L;
        Achievement existingAchievement = Achievement.builder()
                .id(2L)
                .name("Existing Name")
                .milestone("Milestone")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(5)
                .build();
        when(achievementRepository.findById(achievementId)).thenReturn(java.util.Optional.of(sampleAchievement));
        when(achievementRepository.findByName("Existing Name")).thenReturn(java.util.Optional.of(existingAchievement));

        assertThatThrownBy(() -> achievementService.updateAchievement(
                achievementId,
                "Existing Name",
                "Milestone",
                AchievementRequirementType.READING_COUNT,
                5
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sudah ada");

        verify(achievementRepository, never()).save(any());
    }

    @Test
    void updateAchievement_throwsWhenNameIsBlank() {
        Long achievementId = 1L;
        when(achievementRepository.findById(achievementId)).thenReturn(java.util.Optional.of(sampleAchievement));

        assertThatThrownBy(() -> achievementService.updateAchievement(
                achievementId,
                "   ",
                "Milestone",
                AchievementRequirementType.READING_COUNT,
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nama achievement wajib diisi");

        verify(achievementRepository, never()).save(any());
    }

    @Test
    void deleteAchievement_successfullyDeletesAchievementAndRelatedData() {
        Long achievementId = 1L;
        when(achievementRepository.existsById(achievementId)).thenReturn(true);

        achievementService.deleteAchievement(achievementId);

        verify(userAchievementRepository).deleteByAchievementId(achievementId);
        verify(achievementRepository).deleteById(achievementId);
    }

    @Test
    void deleteAchievement_throwsWhenAchievementNotFound() {
        Long nonExistentId = 999L;
        when(achievementRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> achievementService.deleteAchievement(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Achievement tidak ditemukan");

        verify(userAchievementRepository, never()).deleteByAchievementId(any());
        verify(achievementRepository, never()).deleteById(any());
    }

    @Test
    void toggleDisplayAchievement_togglesDisplayStatusSuccessfully() {
        UUID userId = UUID.randomUUID();
        Long achievementId = 1L;
        UserAchievement ua = UserAchievement.builder()
                .id(1L)
                .userId(userId)
                .achievement(sampleAchievement)
                .displayed(false)
                .unlockedAt(LocalDateTime.now())
                .build();
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(ua));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenReturn(ua);

        achievementService.toggleDisplayAchievement(userId, achievementId);

        verify(userAchievementRepository).save(argThat(saved -> saved.isDisplayed() == true));
    }

    @Test
    void toggleDisplayAchievement_throwsWhenUserHasNotUnlockedAchievement() {
        UUID userId = UUID.randomUUID();
        Long achievementId = 999L;
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> achievementService.toggleDisplayAchievement(userId, achievementId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("belum membuka achievement ini");

        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void getAchievementProgress_withUnlockedAchievements() {
        UUID userId = UUID.randomUUID();
        UserStatistic statistic = UserStatistic.builder()
                .userId(userId)
                .totalReadings(5)
                .totalScore(250.0d)
                .build();
        UserAchievement unlockedUa = UserAchievement.builder()
                .id(1L)
                .userId(userId)
                .achievement(sampleAchievement)
                .displayed(true)
                .unlockedAt(LocalDateTime.now())
                .build();
        when(userStatisticRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(statistic));
        when(achievementRepository.findAll()).thenReturn(List.of(sampleAchievement));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(unlockedUa));

        List<AchievementService.AchievementProgress> progress = achievementService.getAchievementProgress(userId);

        assertThat(progress).hasSize(1);
        assertThat(progress.get(0).unlocked()).isTrue();
        assertThat(progress.get(0).displayed()).isTrue();
    }

    @Test
    void createAchievement_throwsWhenMilestoneIsBlank() {
        when(achievementRepository.existsByName("First Steps")).thenReturn(false);

        assertThatThrownBy(() -> achievementService.createAchievement(
                "First Steps",
                "   ",
                AchievementRequirementType.READING_COUNT,
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deskripsi milestone wajib diisi");

        verify(achievementRepository, never()).save(any());
    }

    @Test
    void createAchievement_throwsWhenTargetValueIsNonPositive() {
        when(achievementRepository.existsByName("First Steps")).thenReturn(false);

        assertThatThrownBy(() -> achievementService.createAchievement(
                "First Steps",
                "Complete your first reading",
                AchievementRequirementType.READING_COUNT,
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target achievement harus lebih besar dari nol");

        verify(achievementRepository, never()).save(any());
    }
}
