package id.ac.ui.cs.advprog.yomu.achievement.repository;

import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserMissionProgressRepository extends JpaRepository<UserMissionProgress, Long> {
    Optional<UserMissionProgress> findByUserIdAndMissionId(UUID userId, Long missionId);
    List<UserMissionProgress> findByUserId(UUID userId);
}
