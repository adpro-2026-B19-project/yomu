package id.ac.ui.cs.advprog.yomu.achievement;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserStatistic;
import id.ac.ui.cs.advprog.yomu.achievement.repository.AchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserMissionProgressRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserStatisticRepository;
import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AchievementIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private UserMissionProgressRepository progressRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserStatisticRepository userStatisticRepository;

    private UUID testUserId;
    private DailyMission testMission;
    private Achievement testAchievement;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testMission = DailyMission.builder()
                .title("Misi Baca 1 Teks Test")
                .targetCount(1)
                .activeDate(LocalDate.now())
                .primary(true)
                .build();
        dailyMissionRepository.save(testMission);
        testAchievement = Achievement.builder()
                .name("Pembaca Pemula Test")
                .milestone("Selesaikan 1 bacaan")
                .requirementType(AchievementRequirementType.READING_COUNT)
                .targetValue(1)
                .build();
        achievementRepository.save(testAchievement);
    }

    @Test
    void testQuizCompletionEventUpdatesMissionAndUnlocksAchievement() {
        assertTrue(progressRepository.findByUserId(testUserId).isEmpty(), "Seharusnya belum ada progress misi");
        assertTrue(userStatisticRepository.findByUserId(testUserId).isEmpty(), "Seharusnya statistik membaca belum ada");
        assertTrue(userAchievementRepository.findByUserId(testUserId).isEmpty(), "Seharusnya belum ada achievement yang terbuka");
        QuizCompletedEvent event = new QuizCompletedEvent(
                UUID.randomUUID(), // eventId
                testUserId,        // userId (yang ngerjain)
                UUID.randomUUID(), // textId
                1L,                // readingTextId
                100.0,             // skor
                1.0,               // akurasi
                LocalDateTime.now()// waktu selesai
        );
        eventPublisher.publishEvent(event);
        UserMissionProgress progress = progressRepository.findByUserIdAndMissionId(testUserId, testMission.getId())
                .orElseThrow(() -> new AssertionError("Progress misi tidak ditemukan!"));
        assertEquals(1, progress.getCurrentProgress(), "Progress misi harian harusnya bertambah 1");
        assertTrue(progress.isCompleted(), "Misi harian dengan target 1 seharusnya ditandai selesai");
        UserStatistic statistic = userStatisticRepository.findByUserId(testUserId)
                .orElseThrow(() -> new AssertionError("Statistik user tidak terbuat!"));
        assertEquals(1, statistic.getTotalReadings(), "Total membaca user harusnya 1");
        var unlockedAchievements = userAchievementRepository.findByUserId(testUserId);
        assertEquals(1, unlockedAchievements.size(), "Harusnya ada 1 achievement yang di-unlock");
        assertEquals(testAchievement.getId(), unlockedAchievements.get(0).getAchievement().getId(), "Achievement yang terbuka harus sesuai");
        assertFalse(unlockedAchievements.get(0).isDisplayed(), "Status awal display harus false (belum ditampilkan di profil)");
    }

    @Test
    void testMilestone100_Idempotency_AchievementNotUnlockedTwice() {
        // MILESTONE 100% DELIVERABLE: Idempotency
        // Achievement tidak dapat terbuka dua kali untuk satu pelajar yang sama
        assertTrue(userAchievementRepository.findByUserId(testUserId).isEmpty(), "Awal: belum ada achievement terbuka");
        
        // First quiz completion - should unlock achievement
        QuizCompletedEvent firstEvent = new QuizCompletedEvent(
                UUID.randomUUID(),
                testUserId,
                UUID.randomUUID(),
                1L,
                100.0,
                1.0,
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(firstEvent);

        var achievementsAfterFirst = userAchievementRepository.findByUserId(testUserId);
        assertEquals(1, achievementsAfterFirst.size(), "Setelah event pertama: achievement harus terbuka sekali");

        // Second quiz completion - should NOT unlock the same achievement again
        QuizCompletedEvent secondEvent = new QuizCompletedEvent(
                UUID.randomUUID(),
                testUserId,
                UUID.randomUUID(),
                2L,
                95.0,
                0.95,
                LocalDateTime.now().plusSeconds(1)
        );
        eventPublisher.publishEvent(secondEvent);

        var achievementsAfterSecond = userAchievementRepository.findByUserId(testUserId);
        assertEquals(1, achievementsAfterSecond.size(), "Setelah event kedua: achievement tetap terbuka sekali (idempotent)");
        assertEquals(achievementsAfterFirst.get(0).getId(), achievementsAfterSecond.get(0).getId(), "ID achievement harus sama (tidak ada duplikasi)");
    }

    @Test
    void testMilestone100_EdgeCase_DailyMissionCreatedMidDay() {
        // MILESTONE 100% DELIVERABLE: Edge Case
        // Admin membuat daily mission baru di tengah hari → pelajar yang sudah aktif hari itu
        // mendapat misi baru tanpa kehilangan progres yang sudah ada
        
        LocalDate today = LocalDate.now();
        dailyMissionRepository.delete(testMission);
        
        // Existing mission for today
        DailyMission existingMission = DailyMission.builder()
                .title("Morning mission")
                .targetCount(2)
                .activeDate(today)
                .primary(true)
                .build();
        dailyMissionRepository.save(existingMission);
        
        // User already made progress on existing mission
        QuizCompletedEvent morningEvent = new QuizCompletedEvent(
                UUID.randomUUID(),
                testUserId,
                UUID.randomUUID(),
                1L,
                100.0,
                1.0,
                LocalDateTime.now().withHour(10).withMinute(0)
        );
        eventPublisher.publishEvent(morningEvent);
        
        var progressBeforeMidDay = progressRepository.findByUserId(testUserId);
        assertEquals(1, progressBeforeMidDay.size(), "Sebelum mid-day: ada 1 progress");
        assertEquals(1, progressBeforeMidDay.get(0).getCurrentProgress(), "Progres morning mission: 1");
        
        // Mid-day: admin creates a new mission
        DailyMission newMission = DailyMission.builder()
                .title("Afternoon mission")
                .targetCount(1)
                .activeDate(today)
                .primary(false)
                .build();
        dailyMissionRepository.save(newMission);
        
        // User completes more quizzes (should affect both missions if applicable)
        QuizCompletedEvent afternoonEvent = new QuizCompletedEvent(
                UUID.randomUUID(),
                testUserId,
                UUID.randomUUID(),
                2L,
                95.0,
                0.95,
                LocalDateTime.now().withHour(14).withMinute(0)
        );
        eventPublisher.publishEvent(afternoonEvent);
        
        var progressAfterMidDay = progressRepository.findByUserId(testUserId);
        // Should have progress for both missions now
        assertTrue(progressAfterMidDay.stream().anyMatch(p -> p.getMission().getTitle().equals("Morning mission")), 
                   "Progres morning mission masih ada");
        assertTrue(progressAfterMidDay.stream().anyMatch(p -> p.getMission().getTitle().equals("Afternoon mission")),
                   "Progres afternoon mission ada (baru)");
        
        // Verify original progress is preserved
        var morningProgress = progressAfterMidDay.stream()
                .filter(p -> p.getMission().getTitle().equals("Morning mission"))
                .findFirst();
        assertTrue(morningProgress.isPresent(), "Morning mission progress ada");
        assertEquals(2, morningProgress.get().getCurrentProgress(), "Morning mission progress naik ke 2 (tidak kehilangan progres)");
    }

    @Test
    void testMilestone100_UIPolish_AchievementDisplayToggle() {
        // Verify that achievements can be toggled for profile display
        // This test ensures the UI polish feature works end-to-end
        
        // First unlock an achievement
        QuizCompletedEvent event = new QuizCompletedEvent(
                UUID.randomUUID(),
                testUserId,
                UUID.randomUUID(),
                1L,
                100.0,
                1.0,
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);

        var unlockedAchievements = userAchievementRepository.findByUserId(testUserId);
        assertEquals(1, unlockedAchievements.size(), "Achievement harus terbuka");
        assertFalse(unlockedAchievements.get(0).isDisplayed(), "Awal: achievement belum ditampilkan");
        
        // Simulate user toggling display status
        var userAchievement = unlockedAchievements.get(0);
        userAchievement.setDisplayed(true);
        userAchievementRepository.save(userAchievement);
        
        // Verify toggle worked
        var displayedAchievements = userAchievementRepository.findByUserIdAndDisplayedTrueOrderByUnlockedAtDesc(testUserId);
        assertEquals(1, displayedAchievements.size(), "1 achievement harus ditampilkan di profil");
        assertTrue(displayedAchievements.get(0).isDisplayed(), "Achievement harus ditandai sebagai displayed");
    }
}
