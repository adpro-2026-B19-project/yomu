package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;

import java.util.List;
import java.util.UUID;

public interface DailyMissionService {
    void incrementProgress(UUID userId);
    List<DailyMission> getTodayMissions();
    List<UserMissionProgress> getUserProgress(UUID userId);
    DailyMission createDailyMission(String title, int targetCount);
    DailyMission updateDailyMission(Long id, String title, int targetCount);
    void deleteDailyMission(Long id);
}
