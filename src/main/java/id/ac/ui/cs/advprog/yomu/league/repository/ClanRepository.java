package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClanRepository extends JpaRepository<Clan, UUID> {
    boolean existsByNameIgnoreCase(String name);

    @Query("""
            select distinct c
            from Clan c
            join fetch c.tier
            left join fetch c.members
            order by c.createdAt desc
            """)
    List<Clan> findAllForListing();

    @Query("""
            select distinct c
            from Clan c
            join fetch c.tier
            left join fetch c.members
            where c.id = :clanId
            """)
    Optional<Clan> findByIdForDetail(UUID clanId);

    @Query("""
            select distinct c
            from Clan c
            join fetch c.tier
            left join fetch c.members
            where c.tier.code = :tierCode
            order by c.createdAt asc
            """)
    List<Clan> findAllByTierCodeForLeaderboard(@Param("tierCode") TierCode tierCode);

    @Query("""
            select distinct c
            from Clan c
            join fetch c.tier
            left join fetch c.members
            """)
    List<Clan> findAllWithTierAndMembers();

    @Modifying
    @Query("""
            update Clan c
            set c.bronzeScore = c.bronzeScore + :delta
            where c.id = :clanId
            """)
    int incrementBronzeScore(@Param("clanId") UUID clanId, @Param("delta") double delta);
}
