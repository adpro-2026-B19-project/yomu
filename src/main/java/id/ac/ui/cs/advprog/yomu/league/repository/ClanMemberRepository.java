package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClanMemberRepository extends JpaRepository<ClanMember, UUID> {
    boolean existsByUserId(UUID userId);

    boolean existsByClanIdAndUserId(UUID clanId, UUID userId);

    Optional<ClanMember> findByClanIdAndUserId(UUID clanId, UUID userId);

    Optional<ClanMember> findByUserId(UUID userId);

    @Query("""
            select cm
            from ClanMember cm
            join fetch cm.clan c
            join fetch c.tier
            where cm.userId = :userId
            """)
    Optional<ClanMember> findByUserIdWithClan(@Param("userId") UUID userId);
}
