package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AchievementService {
    List<Achievement> getAllAchievements();
    List<UserAchievement> getAchievementsByUserId(UUID userId);
    List<AchievementDistribution> getAchievementDistribution();
    List<AchievementProgress> getAchievementProgress(UUID userId);
    Achievement createAchievement(String name, String milestone, AchievementRequirementType requirementType, int targetValue);
    Achievement updateAchievement(Long id, String name, String milestone, AchievementRequirementType requirementType, int targetValue);
    void deleteAchievement(Long id);
    void toggleDisplayAchievement(UUID userId, Long achievementId);
    void processQuizCompletion(UUID userId, double score, LocalDateTime completedAt);

    record AchievementDistribution(
            Long achievementId,
            String achievementName,
            String milestone,
            long unlockedUserCount
    ) {
    }

    record AchievementProgress(
            Long achievementId,
            String achievementName,
            String milestone,
            AchievementRequirementType requirementType,
            int targetValue,
            double currentValue,
            int progressPercent,
            boolean unlocked,
            boolean displayed
    ) {
    }
}
