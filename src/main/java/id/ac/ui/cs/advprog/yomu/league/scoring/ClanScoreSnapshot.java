package id.ac.ui.cs.advprog.yomu.league.scoring;

import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ClanScoreSnapshot(
        Clan clan,
        List<UUID> currentMemberUserIds,
        List<UUID> weightedMemberUserIds,
        double totalSeasonScore,
        double averageAccuracy,
        Map<UUID, Double> seasonScoreByUserId,
        Map<UUID, Long> weeklyCompletionCountByUserId
) {

    public static ClanScoreSnapshot from(Clan clan, List<ClanQuizScoreEvent> seasonEvents) {
        Set<UUID> currentMemberUserIds = clan.getMembers().stream()
                .map(member -> member.getUserId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> weightedMemberUserIds = new LinkedHashSet<>(currentMemberUserIds);

        double totalSeasonScore = 0.0d;
        double totalAccuracy = 0.0d;
        Map<UUID, Double> seasonScoreByUserId = new java.util.HashMap<>();
        Map<UUID, Long> weeklyCompletionCountByUserId = new java.util.HashMap<>();

        java.time.LocalDateTime weeklyThreshold = java.time.LocalDateTime.now().minusDays(7);
        for (ClanQuizScoreEvent event : seasonEvents) {
            weightedMemberUserIds.add(event.getUserId());
            totalSeasonScore += event.getScore();
            totalAccuracy += event.getAccuracy();
            seasonScoreByUserId.merge(event.getUserId(), event.getScore(), Double::sum);
            if (!event.getCompletedAt().isBefore(weeklyThreshold)) {
                weeklyCompletionCountByUserId.merge(event.getUserId(), 1L, Long::sum);
            }
        }

        double averageAccuracy = seasonEvents.isEmpty() ? 0.0d : totalAccuracy / seasonEvents.size();
        return new ClanScoreSnapshot(
                clan,
                List.copyOf(currentMemberUserIds),
                List.copyOf(weightedMemberUserIds),
                totalSeasonScore,
                averageAccuracy,
                Map.copyOf(seasonScoreByUserId),
                Map.copyOf(weeklyCompletionCountByUserId)
        );
    }

    public boolean hasSeasonActivity() {
        return totalSeasonScore > 0.0d || !seasonScoreByUserId.isEmpty();
    }

    public long activeMemberCount() {
        return memberIdsForWeighting().stream()
                .filter(this::isActiveMember)
                .count();
    }

    public double averageWeeklyCompletionsPerActiveMember() {
        long activeMemberCount = activeMemberCount();
        if (activeMemberCount == 0L) {
            return 0.0d;
        }

        long totalWeeklyCompletions = memberIdsForWeighting().stream()
                .mapToLong(memberId -> weeklyCompletionCountByUserId.getOrDefault(memberId, 0L))
                .sum();
        return (double) totalWeeklyCompletions / activeMemberCount;
    }

    public double weightedAverage(double activeWeight, double inactiveWeight) {
        Collection<UUID> memberIds = memberIdsForWeighting();
        if (memberIds.isEmpty()) {
            return 0.0d;
        }

        double weightedScoreTotal = 0.0d;
        double weightTotal = 0.0d;
        for (UUID memberId : memberIds) {
            double weight = isActiveMember(memberId) ? activeWeight : inactiveWeight;
            weightedScoreTotal += seasonScoreByUserId.getOrDefault(memberId, 0.0d) * weight;
            weightTotal += weight;
        }
        return weightTotal == 0.0d ? 0.0d : weightedScoreTotal / weightTotal;
    }

    private boolean isActiveMember(UUID memberId) {
        return weeklyCompletionCountByUserId.getOrDefault(memberId, 0L) > 0L;
    }

    private Collection<UUID> memberIdsForWeighting() {
        return currentMemberUserIds.isEmpty() ? weightedMemberUserIds : currentMemberUserIds;
    }
}
