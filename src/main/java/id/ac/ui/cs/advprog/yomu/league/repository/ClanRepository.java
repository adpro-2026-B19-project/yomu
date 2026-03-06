package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
