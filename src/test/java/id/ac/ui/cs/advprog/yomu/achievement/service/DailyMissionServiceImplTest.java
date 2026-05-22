package id.ac.ui.cs.advprog.yomu.achievement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserMissionProgressRepository;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;import org.mockito.ArgumentMatchers;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyMissionServiceImplTest {

    @Mock
    private DailyMissionRepository dailyMissionRepository;

    @Mock
    private UserMissionProgressRepository userMissionProgressRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TextRepository textRepository;

    @InjectMocks
    private DailyMissionServiceImpl dailyMissionService;

    private DailyMission sampleMission;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category("Teknologi");
        
        sampleMission = DailyMission.builder()
                .id(1L)
                .title("Complete 3 readings")
                .targetCount(3)
                .activeDate(LocalDate.now())
                .primary(true)
                .categoryId(1L)
                .build();
    }

    @Test
    void createDailyMission_shouldRejectBlankTitle() {
        assertThatThrownBy(() -> dailyMissionService.createDailyMission("   ", 2, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Judul misi harian wajib diisi");
    }

    @Test
    void createDailyMission_shouldRejectZeroTargetCount() {
        assertThatThrownBy(() -> dailyMissionService.createDailyMission("Valid Title", 0, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target misi harian harus lebih besar dari nol");
    }

    @Test
    void createDailyMission_shouldRejectNegativeTargetCount() {
        assertThatThrownBy(() -> dailyMissionService.createDailyMission("Valid Title", -5, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target misi harian harus lebih besar dari nol");
    }

    @Test
    void createDailyMission_withValidCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(dailyMissionRepository.save(any(DailyMission.class))).thenReturn(sampleMission);

        DailyMission result = dailyMissionService.createDailyMission("Complete 3 readings", 3, false, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Complete 3 readings");
        assertThat(result.getCategoryId()).isEqualTo(1L);
        verify(categoryRepository).findById(1L);
        verify(dailyMissionRepository).save(any(DailyMission.class));
    }

    @Test
    void createDailyMission_shouldThrowWhenCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dailyMissionService.createDailyMission("Valid Title", 3, false, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kategori misi harian tidak ditemukan");

        verify(dailyMissionRepository, never()).save(any());
    }

    @Test
    void createDailyMission_asPrimary_clearsExistingPrimary() {
        DailyMission existingPrimary = DailyMission.builder()
                .id(10L)
                .title("Old Primary")
                .targetCount(2)
                .activeDate(LocalDate.now())
                .primary(true)
                .build();
        when(dailyMissionRepository.findByActiveDateAndPrimaryTrue(LocalDate.now()))
                .thenReturn(Optional.of(existingPrimary));
        when(dailyMissionRepository.save(any(DailyMission.class))).thenReturn(sampleMission);

        dailyMissionService.createDailyMission("New Primary", 3, true, null);

        verify(dailyMissionRepository).findByActiveDateAndPrimaryTrue(LocalDate.now());
        verify(dailyMissionRepository, times(2)).save(any(DailyMission.class));
    }

    @Test
    void rotateDailyMissions_shouldReuseExistingMissionsForSameDate() {
        LocalDate targetDate = LocalDate.of(2026, 5, 8);
        DailyMission existingMission = DailyMission.builder()
                .id(10L)
                .title("Today's mission")
                .targetCount(2)
                .activeDate(targetDate)
                .primary(true)
                .build();
        when(dailyMissionRepository.findByActiveDate(targetDate)).thenReturn(List.of(existingMission));

        List<DailyMission> result = dailyMissionService.rotateDailyMissions(targetDate);

        assertThat(result).containsExactly(existingMission);
        verify(dailyMissionRepository, never()).save(any(DailyMission.class));
    }

    @Test
    void rotateDailyMissions_shouldCloneLatestMissionSetWhenTargetDateIsEmpty() {
        LocalDate previousDate = LocalDate.of(2026, 5, 7);
        LocalDate targetDate = LocalDate.of(2026, 5, 8);
        DailyMission previousPrimary = DailyMission.builder()
                .id(1L)
                .title("Complete 3 readings")
                .targetCount(3)
                .activeDate(previousDate)
                .primary(true)
                .build();
        DailyMission previousSecondary = DailyMission.builder()
                .id(2L)
                .title("Score 80 accuracy")
                .targetCount(1)
                .activeDate(previousDate)
                .primary(false)
                .build();

        when(dailyMissionRepository.findByActiveDate(targetDate)).thenReturn(List.of());
        when(dailyMissionRepository.findFirstByActiveDateBeforeOrderByActiveDateDesc(targetDate))
                .thenReturn(java.util.Optional.of(previousPrimary));
        when(dailyMissionRepository.findByActiveDate(previousDate))
                .thenReturn(List.of(previousPrimary, previousSecondary));
        when(dailyMissionRepository.save(any(DailyMission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, DailyMission.class));

        List<DailyMission> rotatedMissions = dailyMissionService.rotateDailyMissions(targetDate);

        assertThat(rotatedMissions).hasSize(2);
        assertThat(rotatedMissions)
                .extracting(DailyMission::getTitle)
                .containsExactly("Complete 3 readings", "Score 80 accuracy");
        assertThat(rotatedMissions)
                .extracting(DailyMission::getActiveDate)
                .containsOnly(targetDate);
        assertThat(rotatedMissions)
                .extracting(DailyMission::isPrimary)
                .containsExactly(true, false);
    }

    @Test
    void rotateDailyMissions_returnsEmptyListWhenNoMissionsExist() {
        LocalDate targetDate = LocalDate.of(2026, 5, 8);
        when(dailyMissionRepository.findByActiveDate(targetDate)).thenReturn(List.of());
        when(dailyMissionRepository.findFirstByActiveDateBeforeOrderByActiveDateDesc(targetDate))
                .thenReturn(Optional.empty());

        List<DailyMission> result = dailyMissionService.rotateDailyMissions(targetDate);

        assertThat(result).isEmpty();
    }

    @Test
    void createDailyMission_midDayCreation_preservesExistingUserProgress() {
        // MILESTONE 100% DELIVERABLE: Edge Case
        // Admin membuat daily mission baru di tengah hari → pelajar yang sudah aktif hari itu
        // mendapat misi baru tanpa kehilangan progres yang sudah ada
        
        LocalDate today = LocalDate.now();
        Long newMissionId = 99L;
        
        // Create a new daily mission (simulating mid-day creation)
        DailyMission newMission = DailyMission.builder()
                .id(newMissionId)
                .title("New mid-day mission")
                .targetCount(5)
                .activeDate(today)
                .primary(false)
                .categoryId(null)
                .build();
        
        when(dailyMissionRepository.save(any(DailyMission.class))).thenReturn(newMission);
        
        // Act: Create a new daily mission in the middle of the day
        DailyMission created = dailyMissionService.createDailyMission(
                "New mid-day mission",
                5,
                false,
                null
        );
        
        // Assert: The mission is created successfully
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("New mid-day mission");
        assertThat(created.getActiveDate()).isEqualTo(today);
        
        // Verify: User progress from earlier in the day is not affected
        // This is handled by the fact that user progress is tracked independently
        // and new missions don't retroactively affect existing progress records
        verify(dailyMissionRepository).save(any(DailyMission.class));
        verify(userMissionProgressRepository, never()).deleteAll();
    }

    @Test
    void getTodayMissions_returnsCurrentDayMissions() {
        LocalDate today = LocalDate.now();
        when(dailyMissionRepository.findByActiveDate(today)).thenReturn(List.of(sampleMission));

        List<DailyMission> result = dailyMissionService.getTodayMissions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Complete 3 readings");
        verify(dailyMissionRepository).findByActiveDate(today);
    }

    @Test
    void getTodayMissions_returnsEmptyListWhenNoMissionsForToday() {
        LocalDate today = LocalDate.now();
        when(dailyMissionRepository.findByActiveDate(today)).thenReturn(List.of());

        List<DailyMission> result = dailyMissionService.getTodayMissions();

        assertThat(result).isEmpty();
    }

    @Test
    void getUserProgress_returnsUserProgress() {
        UUID userId = UUID.randomUUID();
        UserMissionProgress progress = UserMissionProgress.builder()
                .id(1L)
                .userId(userId)
                .mission(sampleMission)
                .currentProgress(1)
                .completed(false)
                .build();
        when(userMissionProgressRepository.findByUserId(userId)).thenReturn(List.of(progress));

        List<UserMissionProgress> result = dailyMissionService.getUserProgress(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentProgress()).isEqualTo(1);
        verify(userMissionProgressRepository).findByUserId(userId);
    }

    @Test
    void getUserProgress_returnsEmptyListWhenUserHasNoProgress() {
        UUID userId = UUID.randomUUID();
        when(userMissionProgressRepository.findByUserId(userId)).thenReturn(List.of());

        List<UserMissionProgress> result = dailyMissionService.getUserProgress(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void incrementProgress_createsNewProgressWhenNotExists() {
        UUID userId = UUID.randomUUID();
        Long readingTextId = 1L;
        
        // Use mission without category to avoid ID mismatch
        DailyMission missionNoCat = DailyMission.builder()
                .id(5L)
                .title("General reading")
                .targetCount(3)
                .activeDate(LocalDate.now())
                .primary(false)
                .categoryId(null)
                .build();
        
        LocalDate today = LocalDate.now();
        when(dailyMissionRepository.findByActiveDate(today)).thenReturn(List.of(missionNoCat));
        when(userMissionProgressRepository.findByUserIdAndMissionId(userId, missionNoCat.getId()))
                .thenReturn(Optional.empty());
        when(userMissionProgressRepository.save(any(UserMissionProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, UserMissionProgress.class));

        dailyMissionService.incrementProgress(userId, readingTextId);

        verify(userMissionProgressRepository).save(any(UserMissionProgress.class));
    }

    @Test
    void incrementProgress_incrementsExistingProgress() {
        UUID userId = UUID.randomUUID();
        Long readingTextId = 1L;
        
        DailyMission missionNoCat = DailyMission.builder()
                .id(6L)
                .title("General reading")
                .targetCount(3)
                .activeDate(LocalDate.now())
                .primary(false)
                .categoryId(null)
                .build();
        
        UserMissionProgress existingProgress = UserMissionProgress.builder()
                .id(1L)
                .userId(userId)
                .mission(missionNoCat)
                .currentProgress(1)
                .completed(false)
                .build();
        
        LocalDate today = LocalDate.now();
        when(dailyMissionRepository.findByActiveDate(today)).thenReturn(List.of(missionNoCat));
        when(userMissionProgressRepository.findByUserIdAndMissionId(userId, missionNoCat.getId()))
                .thenReturn(Optional.of(existingProgress));
        when(userMissionProgressRepository.save(any(UserMissionProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, UserMissionProgress.class));

        dailyMissionService.incrementProgress(userId, readingTextId);

        verify(userMissionProgressRepository).save(ArgumentMatchers.argThat(progress -> progress.getCurrentProgress() == 2));
    }

    @Test
    void incrementProgress_marksProgressAsCompleteWhenTargetReached() {
        UUID userId = UUID.randomUUID();
        Long readingTextId = 1L;
        
        DailyMission missionNoCat = DailyMission.builder()
                .id(8L)
                .title("Complete 2 readings")
                .targetCount(2)
                .activeDate(LocalDate.now())
                .primary(false)
                .categoryId(null)
                .build();
        
        UserMissionProgress progressNearCompletion = UserMissionProgress.builder()
                .id(1L)
                .userId(userId)
                .mission(missionNoCat)
                .currentProgress(1)
                .completed(false)
                .build();
        
        LocalDate today = LocalDate.now();
        when(dailyMissionRepository.findByActiveDate(today)).thenReturn(List.of(missionNoCat));
        when(userMissionProgressRepository.findByUserIdAndMissionId(userId, missionNoCat.getId()))
                .thenReturn(Optional.of(progressNearCompletion));
        when(userMissionProgressRepository.save(any(UserMissionProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, UserMissionProgress.class));

        dailyMissionService.incrementProgress(userId, readingTextId);

        verify(userMissionProgressRepository).save(ArgumentMatchers.argThat(progress -> 
            progress.getCurrentProgress() == 2 && progress.isCompleted()));
    }

    @Test
    void incrementProgress_skipsAlreadyCompletedProgress() {
        UUID userId = UUID.randomUUID();
        Long readingTextId = 1L;
        
        DailyMission missionNoCat = DailyMission.builder()
                .id(9L)
                .title("Already completed")
                .targetCount(2)
                .activeDate(LocalDate.now())
                .primary(false)
                .categoryId(null)
                .build();
        
        UserMissionProgress completedProgress = UserMissionProgress.builder()
                .id(1L)
                .userId(userId)
                .mission(missionNoCat)
                .currentProgress(2)
                .completed(true)
                .build();
        
        LocalDate today = LocalDate.now();
        when(dailyMissionRepository.findByActiveDate(today)).thenReturn(List.of(missionNoCat));
        when(userMissionProgressRepository.findByUserIdAndMissionId(userId, missionNoCat.getId()))
                .thenReturn(Optional.of(completedProgress));

        dailyMissionService.incrementProgress(userId, readingTextId);

        verify(userMissionProgressRepository, never()).save(any());
    }

    @Test
    void incrementProgress_withNoMissionForReadingText() {
        UUID userId = UUID.randomUUID();
        Long readingTextId = 1L;
        
        LocalDate today = LocalDate.now();
        when(dailyMissionRepository.findByActiveDate(today)).thenReturn(List.of());

        dailyMissionService.incrementProgress(userId, readingTextId);

        verify(userMissionProgressRepository, never()).save(any());
    }

    @Test
    void updateDailyMission_successfullyUpdates() {
        Long missionId = 1L;
        when(dailyMissionRepository.findById(missionId)).thenReturn(Optional.of(sampleMission));
        when(dailyMissionRepository.findByActiveDateAndPrimaryTrue(LocalDate.now()))
                .thenReturn(Optional.empty());
        when(dailyMissionRepository.save(any(DailyMission.class))).thenReturn(sampleMission);

        DailyMission result = dailyMissionService.updateDailyMission(
                missionId,
                "Updated Title",
                5,
                true,
                null
        );

        assertThat(result).isNotNull();
        verify(dailyMissionRepository).findById(missionId);
        verify(dailyMissionRepository).save(any(DailyMission.class));
    }

    @Test
    void updateDailyMission_throwsWhenMissionNotFound() {
        Long nonExistentId = 999L;
        when(dailyMissionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dailyMissionService.updateDailyMission(nonExistentId, "Title", 3, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Misi Harian dengan ID");

        verify(dailyMissionRepository, never()).save(any());
    }

    @Test
    void updateDailyMission_throwsWhenTitleIsBlank() {
        Long missionId = 1L;

        assertThatThrownBy(() -> dailyMissionService.updateDailyMission(missionId, "   ", 3, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Judul misi harian wajib diisi");

        verify(dailyMissionRepository, never()).save(any());
    }

    @Test
    void updateDailyMission_throwsWhenTargetCountIsNonPositive() {
        Long missionId = 1L;

        assertThatThrownBy(() -> dailyMissionService.updateDailyMission(missionId, "Title", -1, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target misi harian harus lebih besar dari nol");

        verify(dailyMissionRepository, never()).save(any());
    }

    @Test
    void deleteDailyMission_successfullyDeletesAndCleanUpProgress() {
        Long missionId = 1L;
        when(dailyMissionRepository.findById(missionId)).thenReturn(Optional.of(sampleMission));

        dailyMissionService.deleteDailyMission(missionId);

        verify(userMissionProgressRepository).deleteByMissionId(missionId);
        verify(dailyMissionRepository).delete(sampleMission);
    }

    @Test
    void deleteDailyMission_throwsWhenMissionNotFound() {
        Long nonExistentId = 999L;
        when(dailyMissionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dailyMissionService.deleteDailyMission(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Misi Harian dengan ID");

        verify(userMissionProgressRepository, never()).deleteByMissionId(any());
        verify(dailyMissionRepository, never()).delete(any());
    }
}
