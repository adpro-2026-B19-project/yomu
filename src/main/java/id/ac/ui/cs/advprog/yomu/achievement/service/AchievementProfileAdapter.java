package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import id.ac.ui.cs.advprog.yomu.integration.profile.AchievementProfilePort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AchievementProfileAdapter implements AchievementProfilePort {

    private final UserAchievementRepository userAchievementRepository;

    public AchievementProfileAdapter(UserAchievementRepository userAchievementRepository) {
        this.userAchievementRepository = userAchievementRepository;
    }

    @Override
    public List<DisplayedAchievement> getDisplayedAchievements(UUID userId) {
        return userAchievementRepository.findByUserIdAndDisplayedTrueOrderByUnlockedAtDesc(userId)
                .stream()
                .map(userAchievement -> new DisplayedAchievement(
                        userAchievement.getAchievement().getId(),
                        userAchievement.getAchievement().getName(),
                        userAchievement.getAchievement().getMilestone(),
                        userAchievement.getUnlockedAt()
                ))
                .toList();
    }
}
