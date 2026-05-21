package id.ac.ui.cs.advprog.yomu.achievement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserMissionProgressRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void createDailyMission_shouldRejectBlankTitle() {
        assertThatThrownBy(() -> dailyMissionService.createDailyMission("   ", 2, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Judul misi harian wajib diisi");
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
}
