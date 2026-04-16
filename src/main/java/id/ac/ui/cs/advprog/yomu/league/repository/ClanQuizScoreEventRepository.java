package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClanQuizScoreEventRepository extends JpaRepository<ClanQuizScoreEvent, UUID> {
    boolean existsByEventId(UUID eventId);

    @Query("""
            select count(e) as completedQuizCount,
                   coalesce(sum(e.score), 0) as totalScore,
                   coalesce(avg(e.accuracy), 0) as averageAccuracy
            from ClanQuizScoreEvent e
            where e.userId = :userId
            """)
    Optional<UserQuizStats> summarizeByUserId(@Param("userId") UUID userId);

    interface UserQuizStats {
        long getCompletedQuizCount();

        double getTotalScore();

        double getAverageAccuracy();
    }
}
