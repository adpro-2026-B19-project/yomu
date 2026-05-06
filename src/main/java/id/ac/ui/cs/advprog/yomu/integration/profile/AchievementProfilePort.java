package id.ac.ui.cs.advprog.yomu.integration.profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AchievementProfilePort {
    List<DisplayedAchievement> getDisplayedAchievements(UUID userId);

    record DisplayedAchievement(
            Long achievementId,
            String name,
            String milestone,
            LocalDateTime unlockedAt
    ) {
    }
}
