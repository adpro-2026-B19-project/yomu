package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequest;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClanJoinRequestRepository extends JpaRepository<ClanJoinRequest, UUID> {
    boolean existsByClanIdAndRequesterUserIdAndStatus(UUID clanId, UUID requesterUserId, ClanJoinRequestStatus status);

    Optional<ClanJoinRequest> findByIdAndClanId(UUID id, UUID clanId);

    List<ClanJoinRequest> findByClanIdAndStatusOrderByCreatedAtAsc(UUID clanId, ClanJoinRequestStatus status);
}
