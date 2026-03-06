package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TierRepository extends JpaRepository<Tier, UUID> {
    Optional<Tier> findByCode(TierCode code);
}
