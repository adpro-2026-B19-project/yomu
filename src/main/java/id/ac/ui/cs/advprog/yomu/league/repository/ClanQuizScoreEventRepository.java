package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClanQuizScoreEventRepository extends JpaRepository<ClanQuizScoreEvent, UUID> {
    boolean existsByEventId(UUID eventId);

    List<ClanQuizScoreEvent> findBySeasonIdAndClanIdIn(UUID seasonId, Collection<UUID> clanIds);

    List<ClanQuizScoreEvent> findBySeasonIdAndClanId(UUID seasonId, UUID clanId);

    Optional<ClanQuizScoreEvent> findFirstBySeasonIdOrderByCompletedAtDesc(UUID seasonId);
}
