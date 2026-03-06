package id.ac.ui.cs.advprog.yomu.league.service;

import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMemberRole;
import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClanServiceImpl implements ClanService {

    private static final int MAX_CLAN_NAME_LENGTH = 60;

    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final TierRepository tierRepository;

    public ClanServiceImpl(
            ClanRepository clanRepository,
            ClanMemberRepository clanMemberRepository,
            TierRepository tierRepository
    ) {
        this.clanRepository = clanRepository;
        this.clanMemberRepository = clanMemberRepository;
        this.tierRepository = tierRepository;
    }

    @Override
    @Transactional
    public ClanSummary createClan(CreateClanRequest request, UUID creatorUserId) {
        String normalizedName = normalize(request.name());
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Clan name is required");
        }
        if (normalizedName.length() > MAX_CLAN_NAME_LENGTH) {
            throw new IllegalArgumentException("Clan name must be at most 60 characters");
        }
        if (creatorUserId == null) {
            throw new IllegalArgumentException("Creator user id is required");
        }
        if (clanRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Clan name already exists");
        }

        Tier defaultTier = tierRepository.findByCode(TierCode.BRONZE)
                .orElseGet(() -> tierRepository.save(new Tier(TierCode.BRONZE, "Bronze")));

        Clan clan = clanRepository.save(new Clan(normalizedName, defaultTier, creatorUserId));
        ClanMember leader = clanMemberRepository.save(new ClanMember(clan, creatorUserId, ClanMemberRole.LEADER));
        clan.addMember(leader);

        return toSummary(clan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClanSummary> listClans() {
        return clanRepository.findAllForListing()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private ClanSummary toSummary(Clan clan) {
        return new ClanSummary(
                clan.getId(),
                clan.getName(),
                clan.getTier().getCode().name(),
                clan.getMembers().size(),
                clan.getCreatedByUserId()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
