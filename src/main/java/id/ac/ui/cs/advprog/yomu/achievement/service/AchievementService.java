package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AchievementService {
    List<Achievement> getAllAchievements();
    List<UserAchievement> getAchievementsByUserId(UUID userId);
    Achievement createAchievement(String name, String milestone);
    void toggleDisplayAchievement(UUID userId, Long achievementId);
    void processQuizCompletion(UUID userId, LocalDateTime completedAt);
    Achievement updateAchievement(Long id, String name, String milestone);
    void deleteAchievement(Long id);
}