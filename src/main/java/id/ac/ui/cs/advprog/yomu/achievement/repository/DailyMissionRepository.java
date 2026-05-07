package id.ac.ui.cs.advprog.yomu.achievement.repository;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyMissionRepository extends JpaRepository<DailyMission, Long> {
    List<DailyMission> findByActiveDate(LocalDate activeDate);
    Optional<DailyMission> findByActiveDateAndPrimaryTrue(LocalDate activeDate);
}
