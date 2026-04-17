package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;

import java.util.List;
import java.util.UUID;

public interface AchievementService {
    List<Achievement> getAllAchievements();
    Achievement createAchievement(String name, String milestone);
    List<UserAchievement> getAchievementsByUserId(UUID userId);
    void toggleDisplayAchievement(UUID userId, Long achievementId);
    void checkAndUnlockAchievements(UUID userId);
}