package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;

import java.util.List;
import java.util.UUID;

public interface DailyMissionService {
    void incrementProgress(UUID userId);
    List<DailyMission> getTodayMissions();
    List<UserMissionProgress> getUserProgress(UUID userId);

    default DailyMission createDailyMission(String title, int targetCount) {
        return createDailyMission(title, targetCount, false);
    }

    DailyMission createDailyMission(String title, int targetCount, boolean primary);

    default DailyMission updateDailyMission(Long id, String title, int targetCount) {
        return updateDailyMission(id, title, targetCount, false);
    }

    DailyMission updateDailyMission(Long id, String title, int targetCount, boolean primary);
    void deleteDailyMission(Long id);
}
