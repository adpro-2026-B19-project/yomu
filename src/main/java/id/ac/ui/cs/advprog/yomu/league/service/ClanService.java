package id.ac.ui.cs.advprog.yomu.league.service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;

public interface ClanService {
    ClanSummary createClan(CreateClanRequest request, UUID creatorUserId);

    List<ClanSummary> listClans();

    ClanDetail getClanDetail(UUID clanId, UUID viewerUserId);

    void submitJoinRequest(UUID clanId, UUID requesterUserId);

    void reviewJoinRequest(UUID clanId, UUID joinRequestId, UUID reviewerUserId, JoinRequestDecision decision);

    void recordQuizCompletion(QuizCompletionPayload payload);

    void deleteClan(UUID clanId, UUID requesterUserId);

    SeasonTransitionResult endCurrentSeason();

    List<LeaderboardEntry> getLeaderboard(TierCode tierCode);

    List<LeaderboardEntry> getBronzeLeaderboard();

    PublicProfile getPublicProfile(UUID userId);

    record CreateClanRequest(String name) {
    }

    record ClanSummary(UUID id, String name, String tier, long memberCount, UUID createdByUserId) {
    }

    record ClanDetail(
            UUID id,
            String name,
            String tier,
            long memberCount,
            UUID createdByUserId,
            boolean viewerIsMember,
            boolean viewerIsLeader,
            boolean viewerHasPendingRequest,
            List<ClanMemberSummary> members,
            List<JoinRequestSummary> pendingJoinRequests
    ) {
    }

    record ClanMemberSummary(UUID userId, String role) {
    }

    record JoinRequestSummary(UUID id, UUID requesterUserId, LocalDateTime requestedAt) {
    }

    enum JoinRequestDecision {
        APPROVE,
        REJECT
    }

    record QuizCompletionPayload(
            UUID eventId,
            UUID userId,
            UUID textId,
            double score,
            double accuracy,
            LocalDateTime completedAt
    ) {
    }

    record LeaderboardEntry(
            UUID clanId,
            String clanName,
            String tier,
            long memberCount,
            double baseScore,
            double score,
            List<ScoreModifier> activeModifiers,
            String formulaDescription
    ) {
    }

    record ScoreModifier(
            String code,
            String label,
            double multiplier,
            String description
    ) {
    }

    record SeasonTransitionResult(
            int endedSeasonNumber,
            int newSeasonNumber,
            List<TierChange> clanTierChanges
    ) {
    }

    record TierChange(
            UUID clanId,
            String clanName,
            String previousTier,
            String newTier,
            String reason
    ) {
    }

    record PublicProfile(
            UUID userId,
            String username,
            String displayName,
            String role,
            String clanName,
            String clanTier,
            String clanRole,
            double clanScore,
            long completedQuizCount,
            double totalQuizScore,
            double averageAccuracy,
            List<DisplayedAchievement> displayedAchievements
    ) {
    }

    record DisplayedAchievement(
            Long achievementId,
            String name,
            String milestone,
            LocalDateTime unlockedAt
    ) {
    }
}
