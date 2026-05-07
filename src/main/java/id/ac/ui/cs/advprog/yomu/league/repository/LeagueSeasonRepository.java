package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.LeagueSeason;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueSeasonRepository extends JpaRepository<LeagueSeason, UUID> {
    Optional<LeagueSeason> findByActiveTrue();

    Optional<LeagueSeason> findTopByOrderBySeasonNumberDesc();
}
