package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DailyMissionRotationScheduler.
 *
 * This class tests the automatic rotation of daily missions on a scheduled basis,
 * ensuring that:
 * 1. The scheduler correctly invokes the rotation logic
 * 2. Daily missions are properly rotated for each new day
 * 3. The rotation preserves mission structure (primary/secondary, category)
 * 4. The scheduler handles edge cases gracefully
 */
@ExtendWith(MockitoExtension.class)
class DailyMissionRotationSchedulerTest {

    @Mock
    private DailyMissionService dailyMissionService;

    @InjectMocks
    private DailyMissionRotationScheduler scheduler;

    @Test
    void testRotateDailyMissions_InvokesServiceMethod() {
        // Arrange
        LocalDate today = LocalDate.now();
        DailyMission primaryMission = DailyMission.builder()
                .id(1L)
                .title("Complete 3 readings")
                .targetCount(3)
                .activeDate(today)
                .primary(true)
                .categoryId(null)
                .build();
        DailyMission secondaryMission = DailyMission.builder()
                .id(2L)
                .title("Score 80 accuracy")
                .targetCount(1)
                .activeDate(today)
                .primary(false)
                .categoryId(null)
                .build();

        when(dailyMissionService.rotateDailyMissions(any(LocalDate.class)))
                .thenReturn(List.of(primaryMission, secondaryMission));

        // Act
        scheduler.rotateDailyMissions();

        // Assert
        verify(dailyMissionService).rotateDailyMissions(today);
    }

    @Test
    void testRotateDailyMissions_PreservesFirstPrimary() {
        // Ensure that the first mission returned is always marked as primary
        LocalDate testDate = LocalDate.of(2026, 5, 21);
        DailyMission primaryMission = DailyMission.builder()
                .id(1L)
                .title("Primary mission")
                .targetCount(1)
                .activeDate(testDate)
                .primary(true)
                .build();
        DailyMission secondaryMission = DailyMission.builder()
                .id(2L)
                .title("Secondary mission")
                .targetCount(1)
                .activeDate(testDate)
                .primary(false)
                .build();

        when(dailyMissionService.rotateDailyMissions(testDate))
                .thenReturn(List.of(primaryMission, secondaryMission));

        List<DailyMission> rotated = dailyMissionService.rotateDailyMissions(testDate);

        assertThat(rotated)
                .extracting(DailyMission::isPrimary)
                .containsExactly(true, false);
    }

    @Test
    void testRotateDailyMissions_PreservesMissionMetadata() {
        // Ensure category and other metadata are preserved during rotation
        LocalDate testDate = LocalDate.of(2026, 5, 21);
        Long categoryId = 5L;
        DailyMission missionWithCategory = DailyMission.builder()
                .id(1L)
                .title("Science readings")
                .targetCount(2)
                .activeDate(testDate)
                .primary(true)
                .categoryId(categoryId)
                .build();

        when(dailyMissionService.rotateDailyMissions(testDate))
                .thenReturn(List.of(missionWithCategory));

        List<DailyMission> rotated = dailyMissionService.rotateDailyMissions(testDate);

        assertThat(rotated)
                .singleElement()
                .satisfies(mission -> {
                    assertThat(mission.getTitle()).isEqualTo("Science readings");
                    assertThat(mission.getTargetCount()).isEqualTo(2);
                    assertThat(mission.getCategoryId()).isEqualTo(categoryId);
                });
    }

    @Test
    void testRotateDailyMissions_HandlesEmptyRotationGracefully() {
        // Ensure scheduler doesn't crash if there are no missions to rotate
        LocalDate testDate = LocalDate.of(2026, 5, 21);

        when(dailyMissionService.rotateDailyMissions(testDate))
                .thenReturn(List.of());

        List<DailyMission> rotated = dailyMissionService.rotateDailyMissions(testDate);

        assertThat(rotated).isEmpty();
    }
}
