package id.ac.ui.cs.advprog.yomu.league.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClanServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private ClanRepository clanRepository;

    @Mock
    private ClanMemberRepository clanMemberRepository;

    @Mock
    private ClanJoinRequestRepository clanJoinRequestRepository;

    @Mock
    private ClanQuizScoreEventRepository clanQuizScoreEventRepository;

    @Mock
    private TierRepository tierRepository;

    @Mock
    private LeagueSeasonService leagueSeasonService;

    @Mock
    private ClanScoreCalculator clanScoreCalculator;

    @Mock
    private ReadingStatsPort readingStatsPort;

    @Mock
    private AchievementProfilePort achievementProfilePort;

    @InjectMocks
    private ClanServiceImpl clanService;

    @Test
    void createClanShouldPersistClanAndLeaderMembership() {
        UUID creatorUserId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");

        when(clanRepository.existsByNameIgnoreCase("Code Masters")).thenReturn(false);
        when(tierRepository.findByCode(TierCode.BRONZE)).thenReturn(Optional.of(bronze));
        when(clanRepository.save(any(Clan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clanMemberRepository.save(any(ClanMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClanService.ClanSummary created = clanService.createClan(
                new ClanService.CreateClanRequest("  Code Masters  "),
                creatorUserId
        );

        ArgumentCaptor<Clan> clanCaptor = ArgumentCaptor.forClass(Clan.class);
        verify(clanRepository).save(clanCaptor.capture());
        assertThat(clanCaptor.getValue().getName()).isEqualTo("Code Masters");
        assertThat(clanCaptor.getValue().getCreatedByUserId()).isEqualTo(creatorUserId);
        assertThat(clanCaptor.getValue().getTier().getCode()).isEqualTo(TierCode.BRONZE);

        ArgumentCaptor<ClanMember> memberCaptor = ArgumentCaptor.forClass(ClanMember.class);
        verify(clanMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(ClanMemberRole.LEADER);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(creatorUserId);

        assertThat(created.name()).isEqualTo("Code Masters");
        assertThat(created.tier()).isEqualTo("BRONZE");
        assertThat(created.memberCount()).isEqualTo(1);
    }

    @Test
    void submitJoinRequestShouldPersistPendingRequest() {
        UUID clanId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Bronze Squad", bronze, UUID.randomUUID());

        when(clanRepository.findByIdForDetail(clanId)).thenReturn(Optional.of(clan));
        when(clanMemberRepository.existsByUserId(requesterUserId)).thenReturn(false);
        when(clanJoinRequestRepository.existsByClanIdAndRequesterUserIdAndStatus(
                clanId,
                requesterUserId,
                ClanJoinRequestStatus.PENDING
        )).thenReturn(false);

        clanService.submitJoinRequest(clanId, requesterUserId);

        ArgumentCaptor<ClanJoinRequest> requestCaptor = ArgumentCaptor.forClass(ClanJoinRequest.class);
        verify(clanJoinRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getClan()).isEqualTo(clan);
        assertThat(requestCaptor.getValue().getRequesterUserId()).isEqualTo(requesterUserId);
    }

    @Test
    void reviewJoinRequestShouldFailWhenDatabaseDetectsUserAlreadyInAnotherClan() {
        UUID clanId = UUID.randomUUID();
        UUID leaderUserId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        UUID joinRequestId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Bronze Squad", bronze, leaderUserId);
        ClanMember leaderMember = new ClanMember(clan, leaderUserId, ClanMemberRole.LEADER);
        ClanJoinRequest joinRequest = new ClanJoinRequest(clan, requesterUserId);

        when(clanRepository.findByIdForDetail(clanId)).thenReturn(Optional.of(clan));
        when(clanMemberRepository.findByClanIdAndUserId(clanId, leaderUserId)).thenReturn(Optional.of(leaderMember));
        when(clanJoinRequestRepository.findByIdAndClanId(joinRequestId, clanId)).thenReturn(Optional.of(joinRequest));
        when(clanMemberRepository.existsByUserId(requesterUserId)).thenReturn(false);
        when(clanMemberRepository.save(any(ClanMember.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate user"));

        assertThatThrownBy(() -> clanService.reviewJoinRequest(
                clanId,
                joinRequestId,
                leaderUserId,
                ClanService.JoinRequestDecision.APPROVE
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Requester already belongs to another clan");
    }

    @Test
    void recordQuizCompletionShouldPersistSeasonScopedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID clanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID textId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Bronze Squad", bronze, UUID.randomUUID());
        ReflectionTestUtils.setField(clan, "id", clanId);
        ClanMember member = new ClanMember(clan, userId, ClanMemberRole.MEMBER);
        LeagueSeason season = new LeagueSeason(1);
        ReflectionTestUtils.setField(season, "id", seasonId);

        when(clanQuizScoreEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(clanMemberRepository.findByUserId(userId)).thenReturn(Optional.of(member));
        when(leagueSeasonService.getOrCreateActiveSeason()).thenReturn(season);

        clanService.recordQuizCompletion(new ClanService.QuizCompletionPayload(
                eventId,
                userId,
                textId,
                8.5d,
                0.85d,
                LocalDateTime.now().minusMinutes(1)
        ));

        ArgumentCaptor<ClanQuizScoreEvent> eventCaptor = ArgumentCaptor.forClass(ClanQuizScoreEvent.class);
        verify(clanQuizScoreEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(eventCaptor.getValue().getClanId()).isEqualTo(clanId);
        assertThat(eventCaptor.getValue().getSeasonId()).isEqualTo(seasonId);
        assertThat(eventCaptor.getValue().getScore()).isEqualTo(8.5d);
    }

    @Test
    void recordQuizCompletionShouldBeIdempotentWhenEventAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();

        when(clanQuizScoreEventRepository.existsByEventId(eventId)).thenReturn(true);

        clanService.recordQuizCompletion(new ClanService.QuizCompletionPayload(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                6.0d,
                0.6d,
                LocalDateTime.now().minusMinutes(1)
        ));

        verify(clanQuizScoreEventRepository, never()).save(any(ClanQuizScoreEvent.class));
        verify(leagueSeasonService, never()).getOrCreateActiveSeason();
    }

    @Test
    void getLeaderboardShouldSortByCalculatedCurrentSeasonScore() {
        UUID seasonId = UUID.randomUUID();
        LeagueSeason activeSeason = new LeagueSeason(2);
        ReflectionTestUtils.setField(activeSeason, "id", seasonId);

        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan first = new Clan("High Score", bronze, UUID.randomUUID());
        Clan second = new Clan("Lower Score", bronze, UUID.randomUUID());
        ReflectionTestUtils.setField(first, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(second, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.now().minusHours(2));
        ReflectionTestUtils.setField(second, "createdAt", LocalDateTime.now().minusHours(1));
        first.addMember(new ClanMember(first, UUID.randomUUID(), ClanMemberRole.LEADER));
        second.addMember(new ClanMember(second, UUID.randomUUID(), ClanMemberRole.LEADER));

        when(clanRepository.findAllByTierCodeForLeaderboard(TierCode.BRONZE)).thenReturn(List.of(first, second));
        when(leagueSeasonService.findActiveSeason()).thenReturn(activeSeason);
        when(clanQuizScoreEventRepository.findBySeasonIdAndClanIdIn(eq(seasonId), anyList())).thenReturn(List.of());
        when(clanScoreCalculator.calculate(eq(first), anyList())).thenReturn(new CalculatedClanScore(
                15.0d,
                18.0d,
                List.of(new ActiveScoreModifier("PRODUCTIVITY_BUFF", "Productivity Buff", 1.2d, "desc")),
                "Bronze formula"
        ));
        when(clanScoreCalculator.calculate(eq(second), anyList())).thenReturn(new CalculatedClanScore(
                12.0d,
                12.0d,
                List.of(),
                "Bronze formula"
        ));

        List<ClanService.LeaderboardEntry> entries = clanService.getLeaderboard(TierCode.BRONZE);

        assertThat(entries).hasSize(2);
        assertThat(entries.getFirst().clanName()).isEqualTo("High Score");
        assertThat(entries.getFirst().baseScore()).isEqualTo(15.0d);
        assertThat(entries.getFirst().score()).isEqualTo(18.0d);
        assertThat(entries.getFirst().activeModifiers()).hasSize(1);
    }

    @Test
    void getPublicProfileShouldUseReadingStatsAndCalculatedClanScore() {
        UUID userId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        AuthUser user = new AuthUser("alice", "alice@example.com", 81234567890L, "Alice", "secret");
        ReflectionTestUtils.setField(user, "id", userId);

        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Gamma Clan", bronze, UUID.randomUUID());
        ReflectionTestUtils.setField(clan, "id", UUID.randomUUID());
        ClanMember member = new ClanMember(clan, userId, ClanMemberRole.MEMBER);
        LeagueSeason activeSeason = new LeagueSeason(1);
        ReflectionTestUtils.setField(activeSeason, "id", seasonId);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clanMemberRepository.findByUserIdWithClan(userId)).thenReturn(Optional.of(member));
        when(readingStatsPort.getUserReadingStats(userId)).thenReturn(new ReadingStatsPort.UserReadingStats(3L, 0.8d, 17.75d));
        when(achievementProfilePort.getDisplayedAchievements(userId)).thenReturn(List.of(
                new AchievementProfilePort.DisplayedAchievement(1L, "Pinned", "Reach 3 texts", LocalDateTime.now())
        ));
        when(leagueSeasonService.findActiveSeason()).thenReturn(activeSeason);
        when(clanQuizScoreEventRepository.findBySeasonIdAndClanId(seasonId, clan.getId())).thenReturn(List.of());
        when(clanScoreCalculator.calculate(eq(clan), anyList())).thenReturn(new CalculatedClanScore(
                17.75d,
                21.30d,
                List.of(new ActiveScoreModifier("PRODUCTIVITY_BUFF", "Productivity Buff", 1.2d, "desc")),
                "Bronze formula"
        ));

        ClanService.PublicProfile profile = clanService.getPublicProfile(userId);

        assertThat(profile.userId()).isEqualTo(userId);
        assertThat(profile.username()).isEqualTo("alice");
        assertThat(profile.displayName()).isEqualTo("Alice");
        assertThat(profile.clanName()).isEqualTo("Gamma Clan");
        assertThat(profile.clanTier()).isEqualTo("BRONZE");
        assertThat(profile.clanRole()).isEqualTo("MEMBER");
        assertThat(profile.clanScore()).isEqualTo(21.30d);
        assertThat(profile.completedQuizCount()).isEqualTo(3L);
        assertThat(profile.totalQuizScore()).isEqualTo(17.75d);
        assertThat(profile.averageAccuracy()).isEqualTo(0.8d);
        assertThat(profile.displayedAchievements()).hasSize(1);
    }

    @Test
    void endCurrentSeasonShouldApplyPromotionAndDegradationRules() {
        UUID seasonId = UUID.randomUUID();
        LeagueSeason endedSeason = new LeagueSeason(1);
        ReflectionTestUtils.setField(endedSeason, "id", seasonId);

        LeagueSeason nextSeason = new LeagueSeason(2);
        ReflectionTestUtils.setField(nextSeason, "id", UUID.randomUUID());

        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Tier silver = new Tier(TierCode.SILVER, "Silver");
        Tier gold = new Tier(TierCode.GOLD, "Gold");

        Clan bronzeTop = createClanWithId("Bronze Top", bronze);
        Clan bronzeOther = createClanWithId("Bronze Other", bronze);
        Clan bronzeThird = createClanWithId("Bronze Third", bronze);
        Clan bronzeLast = createClanWithId("Bronze Last", bronze);

        Clan silverTop = createClanWithId("Silver Top", silver);
        Clan silverOther = createClanWithId("Silver Other", silver);
        Clan silverThird = createClanWithId("Silver Third", silver);
        Clan silverLast = createClanWithId("Silver Last", silver);

        when(leagueSeasonService.endActiveSeason()).thenReturn(endedSeason);
        when(leagueSeasonService.startNextSeason()).thenReturn(nextSeason);
        when(clanRepository.findAllWithTierAndMembers()).thenReturn(List.of(
                bronzeTop, bronzeOther, bronzeThird, bronzeLast,
                silverTop, silverOther, silverThird, silverLast
        ));
        when(clanRepository.findAllByTierCodeForLeaderboard(TierCode.BRONZE)).thenReturn(List.of(
                bronzeTop, bronzeOther, bronzeThird, bronzeLast
        ));
        when(clanRepository.findAllByTierCodeForLeaderboard(TierCode.SILVER)).thenReturn(List.of(
                silverTop, silverOther, silverThird, silverLast
        ));
        when(clanRepository.findAllByTierCodeForLeaderboard(TierCode.GOLD)).thenReturn(List.of());
        when(clanRepository.findAllByTierCodeForLeaderboard(TierCode.DIAMOND)).thenReturn(List.of());
        when(clanQuizScoreEventRepository.findBySeasonIdAndClanIdIn(eq(seasonId), anyList())).thenReturn(List.of());

        when(clanScoreCalculator.calculate(eq(bronzeTop), anyList())).thenReturn(score(100.0d));
        when(clanScoreCalculator.calculate(eq(bronzeOther), anyList())).thenReturn(score(80.0d));
        when(clanScoreCalculator.calculate(eq(bronzeThird), anyList())).thenReturn(score(60.0d));
        when(clanScoreCalculator.calculate(eq(bronzeLast), anyList())).thenReturn(score(40.0d));
        when(clanScoreCalculator.calculate(eq(silverTop), anyList())).thenReturn(score(90.0d));
        when(clanScoreCalculator.calculate(eq(silverOther), anyList())).thenReturn(score(70.0d));
        when(clanScoreCalculator.calculate(eq(silverThird), anyList())).thenReturn(score(50.0d));
        when(clanScoreCalculator.calculate(eq(silverLast), anyList())).thenReturn(score(30.0d));
        when(tierRepository.findByCode(TierCode.SILVER)).thenReturn(Optional.of(silver));
        when(tierRepository.findByCode(TierCode.BRONZE)).thenReturn(Optional.of(bronze));
        when(tierRepository.findByCode(TierCode.GOLD)).thenReturn(Optional.of(gold));

        ClanService.SeasonTransitionResult result = clanService.endCurrentSeason();

        assertThat(result.endedSeasonNumber()).isEqualTo(1);
        assertThat(result.newSeasonNumber()).isEqualTo(2);
        assertThat(result.clanTierChanges()).extracting(ClanService.TierChange::clanName)
                .containsExactlyInAnyOrder("Bronze Top", "Silver Top", "Silver Last");
        assertThat(bronzeTop.getTier().getCode()).isEqualTo(TierCode.SILVER);
        assertThat(silverTop.getTier().getCode()).isEqualTo(TierCode.GOLD);
        assertThat(silverLast.getTier().getCode()).isEqualTo(TierCode.BRONZE);
        verify(clanRepository).saveAll(anyList());
    }

    private CalculatedClanScore score(double finalScore) {
        return new CalculatedClanScore(finalScore, finalScore, List.of(), "Formula");
    }

    private Clan createClanWithId(String name, Tier tier) {
        Clan clan = new Clan(name, tier, UUID.randomUUID());
        clan.addMember(new ClanMember(clan, UUID.randomUUID(), ClanMemberRole.LEADER));
        ReflectionTestUtils.setField(clan, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(clan, "createdAt", LocalDateTime.now());
        return clan;
    }
}
