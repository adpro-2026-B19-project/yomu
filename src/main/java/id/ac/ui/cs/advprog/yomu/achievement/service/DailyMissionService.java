package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyMissionService {
    void incrementProgress(UUID userId, Long readingTextId);
    List<DailyMission> getTodayMissions();
    List<UserMissionProgress> getUserProgress(UUID userId);

    default DailyMission createDailyMission(String title, int targetCount) {
        return createDailyMission(title, targetCount, false, null);
    }

    default DailyMission createDailyMission(String title, int targetCount, boolean primary) {
        return createDailyMission(title, targetCount, primary, null);
    }

    DailyMission createDailyMission(String title, int targetCount, boolean primary, Long categoryId);

    default DailyMission updateDailyMission(Long id, String title, int targetCount) {
        return updateDailyMission(id, title, targetCount, false, null);
    }

    default DailyMission updateDailyMission(Long id, String title, int targetCount, boolean primary) {
        return updateDailyMission(id, title, targetCount, primary, null);
    }

    DailyMission updateDailyMission(Long id, String title, int targetCount, boolean primary, Long categoryId);
    void deleteDailyMission(Long id);

    default List<DailyMission> rotateDailyMissions() {
        return rotateDailyMissions(LocalDate.now());
    }

    List<DailyMission> rotateDailyMissions(LocalDate targetDate);
}
