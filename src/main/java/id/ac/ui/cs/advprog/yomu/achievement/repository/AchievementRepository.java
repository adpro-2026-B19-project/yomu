package id.ac.ui.cs.advprog.yomu.achievement.repository;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    boolean existsByName(String name);
}