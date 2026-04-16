package id.ac.ui.cs.advprog.yomu.league.service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ClanService {
    ClanSummary createClan(CreateClanRequest request, UUID creatorUserId);

    List<ClanSummary> listClans();

    ClanDetail getClanDetail(UUID clanId, UUID viewerUserId);

    void submitJoinRequest(UUID clanId, UUID requesterUserId);

    void reviewJoinRequest(UUID clanId, UUID joinRequestId, UUID reviewerUserId, JoinRequestDecision decision);

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
}
