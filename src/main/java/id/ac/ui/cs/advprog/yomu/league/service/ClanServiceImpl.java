package id.ac.ui.cs.advprog.yomu.league.service;

import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequest;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequestStatus;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMemberRole;
import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanJoinRequestRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClanServiceImpl implements ClanService {

    private static final int MAX_CLAN_NAME_LENGTH = 60;

    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final ClanJoinRequestRepository clanJoinRequestRepository;
    private final TierRepository tierRepository;

    public ClanServiceImpl(
            ClanRepository clanRepository,
            ClanMemberRepository clanMemberRepository,
            ClanJoinRequestRepository clanJoinRequestRepository,
            TierRepository tierRepository
    ) {
        this.clanRepository = clanRepository;
        this.clanMemberRepository = clanMemberRepository;
        this.clanJoinRequestRepository = clanJoinRequestRepository;
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

    @Override
    @Transactional(readOnly = true)
    public ClanDetail getClanDetail(UUID clanId, UUID viewerUserId) {
        Clan clan = getExistingClan(clanId);
        boolean viewerIsMember = viewerUserId != null
                && clanMemberRepository.existsByClanIdAndUserId(clanId, viewerUserId);
        boolean viewerIsLeader = viewerUserId != null
                && clanMemberRepository.findByClanIdAndUserId(clanId, viewerUserId)
                .map(member -> member.getRole() == ClanMemberRole.LEADER)
                .orElse(false);
        boolean viewerHasPendingRequest = viewerUserId != null
                && clanJoinRequestRepository.existsByClanIdAndRequesterUserIdAndStatus(
                clanId,
                viewerUserId,
                ClanJoinRequestStatus.PENDING
        );

        List<ClanMemberSummary> members = clan.getMembers().stream()
                .sorted(Comparator
                        .comparing((ClanMember member) -> member.getRole() != ClanMemberRole.LEADER)
                        .thenComparing(ClanMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(member -> new ClanMemberSummary(member.getUserId(), member.getRole().name()))
                .toList();

        List<JoinRequestSummary> pendingJoinRequests = List.of();
        if (viewerIsLeader) {
            pendingJoinRequests = clanJoinRequestRepository
                    .findByClanIdAndStatusOrderByCreatedAtAsc(clanId, ClanJoinRequestStatus.PENDING)
                    .stream()
                    .map(request -> new JoinRequestSummary(
                            request.getId(),
                            request.getRequesterUserId(),
                            request.getCreatedAt()
                    ))
                    .toList();
        }

        return new ClanDetail(
                clan.getId(),
                clan.getName(),
                clan.getTier().getCode().name(),
                clan.getMembers().size(),
                clan.getCreatedByUserId(),
                viewerIsMember,
                viewerIsLeader,
                viewerHasPendingRequest,
                members,
                pendingJoinRequests
        );
    }

    @Override
    @Transactional
    public void submitJoinRequest(UUID clanId, UUID requesterUserId) {
        if (requesterUserId == null) {
            throw new IllegalArgumentException("Requester user id is required");
        }

        Clan clan = getExistingClan(clanId);
        if (clanMemberRepository.existsByUserId(requesterUserId)) {
            throw new IllegalArgumentException("User already belongs to a clan");
        }
        if (clanJoinRequestRepository.existsByClanIdAndRequesterUserIdAndStatus(
                clanId,
                requesterUserId,
                ClanJoinRequestStatus.PENDING
        )) {
            throw new IllegalArgumentException("Join request is already pending");
        }

        clanJoinRequestRepository.save(new ClanJoinRequest(clan, requesterUserId));
    }

    @Override
    @Transactional
    public void reviewJoinRequest(UUID clanId, UUID joinRequestId, UUID reviewerUserId, JoinRequestDecision decision) {
        if (reviewerUserId == null) {
            throw new IllegalArgumentException("Reviewer user id is required");
        }
        if (decision == null) {
            throw new IllegalArgumentException("Decision is required");
        }

        Clan clan = getExistingClan(clanId);
        ClanMember reviewer = clanMemberRepository.findByClanIdAndUserId(clanId, reviewerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer is not a clan member"));
        if (reviewer.getRole() != ClanMemberRole.LEADER) {
            throw new IllegalArgumentException("Only clan leader can review join requests");
        }

        ClanJoinRequest joinRequest = clanJoinRequestRepository.findByIdAndClanId(joinRequestId, clanId)
                .orElseThrow(() -> new IllegalArgumentException("Join request was not found"));
        if (joinRequest.getStatus() != ClanJoinRequestStatus.PENDING) {
            throw new IllegalArgumentException("Join request has already been processed");
        }

        if (decision == JoinRequestDecision.APPROVE) {
            if (clanMemberRepository.existsByUserId(joinRequest.getRequesterUserId())) {
                throw new IllegalArgumentException("Requester already belongs to another clan");
            }
            ClanMember member = clanMemberRepository.save(
                    new ClanMember(clan, joinRequest.getRequesterUserId(), ClanMemberRole.MEMBER)
            );
            clan.addMember(member);
            joinRequest.approve(reviewerUserId);
        } else {
            joinRequest.reject(reviewerUserId);
        }

        clanJoinRequestRepository.save(joinRequest);
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

    private Clan getExistingClan(UUID clanId) {
        if (clanId == null) {
            throw new IllegalArgumentException("Clan id is required");
        }
        return clanRepository.findByIdForDetail(clanId)
                .orElseThrow(() -> new IllegalArgumentException("Clan was not found"));
    }
}
