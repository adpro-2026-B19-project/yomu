package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClanQuizScoreEventRepository extends JpaRepository<ClanQuizScoreEvent, UUID> {
    boolean existsByEventId(UUID eventId);
}
