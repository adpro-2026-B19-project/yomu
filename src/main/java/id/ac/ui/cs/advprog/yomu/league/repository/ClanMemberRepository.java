package id.ac.ui.cs.advprog.yomu.league.repository;

import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClanMemberRepository extends JpaRepository<ClanMember, UUID> {
}
