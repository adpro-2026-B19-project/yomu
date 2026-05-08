package id.ac.ui.cs.advprog.yomu.achievement.repository;

import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserId(UUID userId);
    List<UserAchievement> findByUserIdAndDisplayedTrueOrderByUnlockedAtDesc(UUID userId);
    boolean existsByUserIdAndAchievementId(UUID userId, Long achievementId);
    long countByAchievement_Id(Long achievementId);
    void deleteByAchievementId(Long achievementId);
}
