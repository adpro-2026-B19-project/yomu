package id.ac.ui.cs.advprog.yomu.league.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.integration.profile.AchievementProfilePort;
import id.ac.ui.cs.advprog.yomu.integration.reading.ReadingStatsPort;
import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequest;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequestStatus;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMemberRole;
import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import id.ac.ui.cs.advprog.yomu.league.model.LeagueSeason;
import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanJoinRequestRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanQuizScoreEventRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
import id.ac.ui.cs.advprog.yomu.league.scoring.ActiveScoreModifier;
import id.ac.ui.cs.advprog.yomu.league.scoring.CalculatedClanScore;
import id.ac.ui.cs.advprog.yomu.league.scoring.ClanScoreCalculator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClanServiceImpl implements ClanService {

    private static final int MAX_CLAN_NAME_LENGTH = 60;

    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final ClanJoinRequestRepository clanJoinRequestRepository;
    private final ClanQuizScoreEventRepository clanQuizScoreEventRepository;
    private final TierRepository tierRepository;
    private final AuthRepository authRepository;
    private final LeagueSeasonService leagueSeasonService;
    private final ClanScoreCalculator clanScoreCalculator;
    private final ReadingStatsPort readingStatsPort;
    private final AchievementProfilePort achievementProfilePort;

    public ClanServiceImpl(
            ClanRepository clanRepository,
            ClanMemberRepository clanMemberRepository,
            ClanJoinRequestRepository clanJoinRequestRepository,
            ClanQuizScoreEventRepository clanQuizScoreEventRepository,
            TierRepository tierRepository,
            AuthRepository authRepository,
            LeagueSeasonService leagueSeasonService,
            ClanScoreCalculator clanScoreCalculator,
            ReadingStatsPort readingStatsPort,
            AchievementProfilePort achievementProfilePort
    ) {
        this.clanRepository = clanRepository;
        this.clanMemberRepository = clanMemberRepository;
        this.clanJoinRequestRepository = clanJoinRequestRepository;
        this.clanQuizScoreEventRepository = clanQuizScoreEventRepository;
        this.tierRepository = tierRepository;
        this.authRepository = authRepository;
        this.leagueSeasonService = leagueSeasonService;
        this.clanScoreCalculator = clanScoreCalculator;
        this.readingStatsPort = readingStatsPort;
        this.achievementProfilePort = achievementProfilePort;
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

        Clan clan = clanRepository.save(new Clan(normalizedName, resolveTier(TierCode.BRONZE), creatorUserId));
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
            ClanMember member;
            try {
                member = clanMemberRepository.save(
                        new ClanMember(clan, joinRequest.getRequesterUserId(), ClanMemberRole.MEMBER)
                );
            } catch (DataIntegrityViolationException exception) {
                throw new IllegalArgumentException("Requester already belongs to another clan", exception);
            }
            clan.addMember(member);
            joinRequest.approve(reviewerUserId);
        } else {
            joinRequest.reject(reviewerUserId);
        }

        clanJoinRequestRepository.save(joinRequest);
    }

    @Override
    @Transactional
    public void recordQuizCompletion(QuizCompletionPayload payload) {
        validateQuizPayload(payload);

        if (clanQuizScoreEventRepository.existsByEventId(payload.eventId())) {
            return;
        }

        ClanMember member = clanMemberRepository.findByUserId(payload.userId()).orElse(null);
        if (member == null) {
            return;
        }

        LeagueSeason activeSeason = leagueSeasonService.getOrCreateActiveSeason();
        ClanQuizScoreEvent event = new ClanQuizScoreEvent(
                payload.eventId(),
                member.getClan().getId(),
                payload.userId(),
                payload.textId(),
                activeSeason.getId(),
                payload.score(),
                payload.accuracy(),
                payload.completedAt()
        );

        try {
            clanQuizScoreEventRepository.save(event);
        } catch (DataIntegrityViolationException ignoredDuplicate) {
            return;
        }
    }

    @Override
    @Transactional
    public void deleteClan(UUID clanId, UUID requesterUserId) {
        if (requesterUserId == null) {
            throw new IllegalArgumentException("Requester user id is required");
        }

        Clan clan = getExistingClan(clanId);
        ClanMember requesterMembership = clanMemberRepository.findByClanIdAndUserId(clanId, requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("Only clan leader can delete this clan"));
        if (requesterMembership.getRole() != ClanMemberRole.LEADER) {
            throw new IllegalArgumentException("Only clan leader can delete this clan");
        }

        clanJoinRequestRepository.deleteByClanId(clanId);
        clanRepository.delete(clan);
    }

    @Override
    @Transactional
    public SeasonTransitionResult endCurrentSeason() {
        LeagueSeason endedSeason = leagueSeasonService.endActiveSeason();
        Map<TierCode, List<LeaderboardEntry>> standingsByTier = new EnumMap<>(TierCode.class);
        for (TierCode tierCode : TierCode.values()) {
            standingsByTier.put(tierCode, getLeaderboardForSeason(tierCode, endedSeason.getId()));
        }

        Map<UUID, Clan> clansById = clanRepository.findAllWithTierAndMembers().stream()
                .collect(Collectors.toMap(Clan::getId, Function.identity(), (left, right) -> left));
        List<TierChange> plannedChanges = new java.util.ArrayList<>();

        addPromotions(plannedChanges, standingsByTier.get(TierCode.BRONZE), TierCode.SILVER);
        addPromotions(plannedChanges, standingsByTier.get(TierCode.SILVER), TierCode.GOLD);
        addPromotions(plannedChanges, standingsByTier.get(TierCode.GOLD), TierCode.DIAMOND);
        addDegradations(plannedChanges, standingsByTier.get(TierCode.SILVER), TierCode.BRONZE);
        addDegradations(plannedChanges, standingsByTier.get(TierCode.GOLD), TierCode.SILVER);
        addDegradations(plannedChanges, standingsByTier.get(TierCode.DIAMOND), TierCode.GOLD);

        for (TierChange plannedChange : plannedChanges) {
            Clan clan = clansById.get(plannedChange.clanId());
            if (clan != null) {
                clan.changeTier(resolveTier(TierCode.valueOf(plannedChange.newTier())));
            }
        }
        if (!plannedChanges.isEmpty()) {
            clanRepository.saveAll(
                    plannedChanges.stream()
                            .map(change -> clansById.get(change.clanId()))
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .toList()
            );
        }

        LeagueSeason newSeason = leagueSeasonService.startNextSeason();
        return new SeasonTransitionResult(
                endedSeason.getSeasonNumber(),
                newSeason.getSeasonNumber(),
                List.copyOf(plannedChanges)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard(TierCode tierCode) {
        if (tierCode == null) {
            throw new IllegalArgumentException("Tier code is required");
        }

        List<Clan> clans = clanRepository.findAllByTierCodeForLeaderboard(tierCode);
        if (clans.isEmpty()) {
            return List.of();
        }

        LeagueSeason activeSeason = leagueSeasonService.findActiveSeason();
        Map<UUID, List<ClanQuizScoreEvent>> seasonEventsByClanId = activeSeason == null
                ? Map.of()
                : clanQuizScoreEventRepository.findBySeasonIdAndClanIdIn(
                        activeSeason.getId(),
                        clans.stream().map(Clan::getId).toList()
                )
                .stream()
                .collect(Collectors.groupingBy(ClanQuizScoreEvent::getClanId));

        return clans.stream()
                .map(clan -> new ScoredClan(
                        clan,
                        toLeaderboardEntry(
                                clan,
                                clanScoreCalculator.calculate(
                                        clan,
                                        seasonEventsByClanId.getOrDefault(clan.getId(), List.of())
                                )
                        )
                ))
                .sorted(Comparator
                        .comparing((ScoredClan scoredClan) -> scoredClan.entry().score()).reversed()
                        .thenComparing(scoredClan -> scoredClan.clan().getCreatedAt()))
                .map(ScoredClan::entry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getBronzeLeaderboard() {
        return getLeaderboard(TierCode.BRONZE);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfile getPublicProfile(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }

        AuthUser user = authRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));

        ClanMember member = clanMemberRepository.findByUserIdWithClan(userId).orElse(null);
        ReadingStatsPort.UserReadingStats readingStats = readingStatsPort.getUserReadingStats(userId);

        double currentClanScore = 0.0d;
        if (member != null) {
            LeagueSeason activeSeason = leagueSeasonService.findActiveSeason();
            List<ClanQuizScoreEvent> seasonEvents = activeSeason == null
                    ? List.of()
                    : clanQuizScoreEventRepository.findBySeasonIdAndClanId(
                    activeSeason.getId(),
                    member.getClan().getId()
            );
            currentClanScore = clanScoreCalculator.calculate(member.getClan(), seasonEvents).finalScore();
        }

        return new PublicProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                member != null ? member.getClan().getName() : null,
                member != null ? member.getClan().getTier().getCode().name() : null,
                member != null ? member.getRole().name() : null,
                currentClanScore,
                readingStats.totalTextsCompleted(),
                readingStats.totalScore(),
                readingStats.averageAccuracy(),
                achievementProfilePort.getDisplayedAchievements(userId).stream()
                        .map(displayedAchievement -> new DisplayedAchievement(
                                displayedAchievement.achievementId(),
                                displayedAchievement.name(),
                                displayedAchievement.milestone(),
                                displayedAchievement.unlockedAt()
                        ))
                        .toList()
        );
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

    private LeaderboardEntry toLeaderboardEntry(Clan clan, CalculatedClanScore calculatedClanScore) {
        return new LeaderboardEntry(
                clan.getId(),
                clan.getName(),
                clan.getTier().getCode().name(),
                clan.getMembers().size(),
                calculatedClanScore.baseScore(),
                calculatedClanScore.finalScore(),
                calculatedClanScore.activeModifiers().stream()
                        .map(this::toScoreModifier)
                        .toList(),
                calculatedClanScore.formulaDescription()
        );
    }

    private ScoreModifier toScoreModifier(ActiveScoreModifier modifier) {
        return new ScoreModifier(
                modifier.code(),
                modifier.label(),
                modifier.multiplier(),
                modifier.description()
        );
    }

    private Tier resolveTier(TierCode tierCode) {
        return tierRepository.findByCode(tierCode)
                .orElseGet(() -> tierRepository.save(new Tier(tierCode, formatTierDisplayName(tierCode))));
    }

    private List<LeaderboardEntry> getLeaderboardForSeason(TierCode tierCode, UUID seasonId) {
        List<Clan> clans = clanRepository.findAllByTierCodeForLeaderboard(tierCode);
        if (clans.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<ClanQuizScoreEvent>> seasonEventsByClanId = clanQuizScoreEventRepository
                .findBySeasonIdAndClanIdIn(seasonId, clans.stream().map(Clan::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(ClanQuizScoreEvent::getClanId));

        return clans.stream()
                .map(clan -> new ScoredClan(
                        clan,
                        toLeaderboardEntry(
                                clan,
                                clanScoreCalculator.calculate(
                                        clan,
                                        seasonEventsByClanId.getOrDefault(clan.getId(), List.of())
                                )
                        )
                ))
                .sorted(Comparator
                        .comparing((ScoredClan scoredClan) -> scoredClan.entry().score()).reversed()
                        .thenComparing(scoredClan -> scoredClan.clan().getCreatedAt()))
                .map(ScoredClan::entry)
                .toList();
    }

    private void addPromotions(List<TierChange> plannedChanges, List<LeaderboardEntry> standings, TierCode targetTier) {
        int movementCount = calculateMovementCount(standings.size());
        for (int index = 0; index < movementCount; index++) {
            LeaderboardEntry clan = standings.get(index);
            plannedChanges.add(new TierChange(
                    clan.clanId(),
                    clan.clanName(),
                    clan.tier(),
                    targetTier.name(),
                    "PROMOTION"
            ));
        }
    }

    private void addDegradations(List<TierChange> plannedChanges, List<LeaderboardEntry> standings, TierCode targetTier) {
        int movementCount = calculateMovementCount(standings.size());
        for (int index = standings.size() - movementCount; index < standings.size(); index++) {
            if (index < 0) {
                continue;
            }
            LeaderboardEntry clan = standings.get(index);
            plannedChanges.add(new TierChange(
                    clan.clanId(),
                    clan.clanName(),
                    clan.tier(),
                    targetTier.name(),
                    "DEGRADATION"
            ));
        }
    }

    private int calculateMovementCount(int tierSize) {
        int movementCount = (int) Math.floor(tierSize * 0.25d);
        if (movementCount == 0 && tierSize >= 4) {
            return 1;
        }
        return movementCount;
    }

    private String formatTierDisplayName(TierCode tierCode) {
        String normalized = tierCode.name().toLowerCase();
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateQuizPayload(QuizCompletionPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Quiz completion payload is required");
        }
        if (payload.eventId() == null) {
            throw new IllegalArgumentException("Event id is required");
        }
        if (payload.userId() == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (payload.textId() == null) {
            throw new IllegalArgumentException("Text id is required");
        }
        if (Double.isNaN(payload.score()) || Double.isInfinite(payload.score()) || payload.score() < 0.0d) {
            throw new IllegalArgumentException("Score must be a non-negative finite number");
        }
        if (Double.isNaN(payload.accuracy()) || Double.isInfinite(payload.accuracy())) {
            throw new IllegalArgumentException("Accuracy must be a finite number");
        }
        if (payload.completedAt() == null) {
            throw new IllegalArgumentException("Completed timestamp is required");
        }
        if (payload.completedAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new IllegalArgumentException("Completed timestamp cannot be in the far future");
        }
    }

    private Clan getExistingClan(UUID clanId) {
        if (clanId == null) {
            throw new IllegalArgumentException("Clan id is required");
        }
        return clanRepository.findByIdForDetail(clanId)
                .orElseThrow(() -> new IllegalArgumentException("Clan was not found"));
    }

    private record ScoredClan(Clan clan, LeaderboardEntry entry) {
    }
}
