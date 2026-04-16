package id.ac.ui.cs.advprog.yomu.league.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequest;
import id.ac.ui.cs.advprog.yomu.league.model.ClanJoinRequestStatus;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMemberRole;
import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanJoinRequestRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanQuizScoreEventRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
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
        assertThat(created.createdByUserId()).isEqualTo(creatorUserId);
    }

    @Test
    void createClanShouldFailWhenNameAlreadyExists() {
        when(clanRepository.existsByNameIgnoreCase("Code Masters")).thenReturn(true);

        assertThatThrownBy(() -> clanService.createClan(
                new ClanService.CreateClanRequest("Code Masters"),
                UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Clan name already exists");

        verify(clanRepository, never()).save(any(Clan.class));
        verify(clanMemberRepository, never()).save(any(ClanMember.class));
    }

    @Test
    void listClansShouldMapEntityToSummary() {
        UUID creatorUserId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Bronze Squad", bronze, creatorUserId);
        clan.addMember(new ClanMember(clan, creatorUserId, ClanMemberRole.LEADER));
        clan.addMember(new ClanMember(clan, UUID.randomUUID(), ClanMemberRole.MEMBER));

        when(clanRepository.findAllForListing()).thenReturn(List.of(clan));

        List<ClanService.ClanSummary> summaries = clanService.listClans();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().name()).isEqualTo("Bronze Squad");
        assertThat(summaries.getFirst().tier()).isEqualTo("BRONZE");
        assertThat(summaries.getFirst().memberCount()).isEqualTo(2);
        assertThat(summaries.getFirst().createdByUserId()).isEqualTo(creatorUserId);
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
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(ClanJoinRequestStatus.PENDING);
    }

    @Test
    void reviewJoinRequestShouldApproveAndAddMember() {
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
        when(clanMemberRepository.save(any(ClanMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clanService.reviewJoinRequest(
                clanId,
                joinRequestId,
                leaderUserId,
                ClanService.JoinRequestDecision.APPROVE
        );

        ArgumentCaptor<ClanMember> memberCaptor = ArgumentCaptor.forClass(ClanMember.class);
        verify(clanMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(requesterUserId);
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(ClanMemberRole.MEMBER);

        assertThat(joinRequest.getStatus()).isEqualTo(ClanJoinRequestStatus.APPROVED);
        assertThat(joinRequest.getReviewedByUserId()).isEqualTo(leaderUserId);
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
    void recordQuizCompletionShouldIncreaseClanScore() {
        UUID eventId = UUID.randomUUID();
        UUID clanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID textId = UUID.randomUUID();
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Bronze Squad", bronze, UUID.randomUUID());
        ReflectionTestUtils.setField(clan, "id", clanId);
        ClanMember member = new ClanMember(clan, userId, ClanMemberRole.MEMBER);

        when(clanQuizScoreEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(clanMemberRepository.findByUserId(userId)).thenReturn(Optional.of(member));

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
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().getScore()).isEqualTo(8.5d);
        verify(clanRepository).incrementBronzeScore(eq(clanId), eq(8.5d));
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
        verify(clanRepository, never()).incrementBronzeScore(any(UUID.class), anyDouble());
    }

    @Test
    void getBronzeLeaderboardShouldMapScoreAndOrderFromRepository() {
        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan first = new Clan("High Score", bronze, UUID.randomUUID());
        Clan second = new Clan("Lower Score", bronze, UUID.randomUUID());
        first.addMember(new ClanMember(first, UUID.randomUUID(), ClanMemberRole.LEADER));
        second.addMember(new ClanMember(second, UUID.randomUUID(), ClanMemberRole.LEADER));
        ReflectionTestUtils.setField(first, "bronzeScore", 11.0d);
        ReflectionTestUtils.setField(second, "bronzeScore", 4.5d);

        when(clanRepository.findLeaderboardByTierCode(TierCode.BRONZE)).thenReturn(List.of(first, second));

        List<ClanService.LeaderboardEntry> entries = clanService.getBronzeLeaderboard();

        assertThat(entries).hasSize(2);
        assertThat(entries.getFirst().clanName()).isEqualTo("High Score");
        assertThat(entries.getFirst().tier()).isEqualTo("BRONZE");
        assertThat(entries.getFirst().score()).isEqualTo(11.0d);
    }

    @Test
    void getPublicProfileShouldReturnPublicIdentityClanAndStats() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser("alice", "alice@example.com", 81234567890L, "Alice", "secret");
        ReflectionTestUtils.setField(user, "id", userId);

        Tier bronze = new Tier(TierCode.BRONZE, "Bronze");
        Clan clan = new Clan("Gamma Clan", bronze, UUID.randomUUID());
        ReflectionTestUtils.setField(clan, "bronzeScore", 17.75d);
        ClanMember member = new ClanMember(clan, userId, ClanMemberRole.MEMBER);

        ClanQuizScoreEventRepository.UserQuizStats stats = new ClanQuizScoreEventRepository.UserQuizStats() {
            @Override
            public long getCompletedQuizCount() {
                return 3L;
            }

            @Override
            public double getTotalScore() {
                return 17.75d;
            }

            @Override
            public double getAverageAccuracy() {
                return 0.8d;
            }
        };

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clanMemberRepository.findByUserIdWithClan(userId)).thenReturn(Optional.of(member));
        when(clanQuizScoreEventRepository.summarizeByUserId(userId)).thenReturn(Optional.of(stats));

        ClanService.PublicProfile profile = clanService.getPublicProfile(userId);

        assertThat(profile.userId()).isEqualTo(userId);
        assertThat(profile.username()).isEqualTo("alice");
        assertThat(profile.displayName()).isEqualTo("Alice");
        assertThat(profile.clanName()).isEqualTo("Gamma Clan");
        assertThat(profile.clanTier()).isEqualTo("BRONZE");
        assertThat(profile.clanRole()).isEqualTo("MEMBER");
        assertThat(profile.clanScore()).isEqualTo(17.75d);
        assertThat(profile.completedQuizCount()).isEqualTo(3L);
        assertThat(profile.totalQuizScore()).isEqualTo(17.75d);
        assertThat(profile.averageAccuracy()).isEqualTo(0.8d);
    }
}
