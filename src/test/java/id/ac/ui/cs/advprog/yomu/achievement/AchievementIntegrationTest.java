package id.ac.ui.cs.advprog.yomu.achievement;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
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
                .build();
        dailyMissionRepository.save(testMission);
        testAchievement = Achievement.builder()
                .name("Pembaca Pemula Test")
                .milestone("1")
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
}