package id.ac.ui.cs.advprog.yomu.league.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import id.ac.ui.cs.advprog.yomu.integration.dailymission.DailyMissionStatusPort;
import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMemberRole;
import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClanScoreCalculatorTest {

    @Test
    void bronzeShouldUseTotalSeasonScore() {
        Clan clan = createClan(TierCode.BRONZE);
        List<ClanQuizScoreEvent> seasonEvents = List.of(
                createEvent(clan, clan.getMembers().get(0).getUserId(), 40.0d, 0.8d, 1),
                createEvent(clan, clan.getMembers().get(1).getUserId(), 35.0d, 0.7d, 2)
        );

        ClanScoreCalculator calculator = newCalculator(new DailyMissionStatusPort.PrimaryMissionCompletionSummary(false, 2, 0));
        CalculatedClanScore score = calculator.calculate(clan, seasonEvents);

        assertThat(score.baseScore()).isEqualTo(75.0d);
        assertThat(score.finalScore()).isEqualTo(75.0d);
    }

    @Test
    void silverShouldUseWeightedAverageWithActiveBias() {
        Clan clan = createClan(TierCode.SILVER);
        UUID activeUserId = clan.getMembers().get(0).getUserId();
        UUID inactiveUserId = clan.getMembers().get(1).getUserId();

        List<ClanQuizScoreEvent> seasonEvents = List.of(
                createEvent(clan, activeUserId, 100.0d, 0.9d, 1),
                createEvent(clan, activeUserId, 50.0d, 0.9d, 2),
                createEvent(clan, inactiveUserId, 100.0d, 0.9d, 10)
        );

        ClanScoreCalculator calculator = newCalculator(new DailyMissionStatusPort.PrimaryMissionCompletionSummary(false, 2, 0));
        CalculatedClanScore score = calculator.calculate(clan, seasonEvents);

        assertThat(score.baseScore()).isEqualTo((150.0d * 1.25d + 100.0d * 1.0d) / 2.25d);
    }

    @Test
    void goldShouldUseWeightedAverageWithStrongerActiveBias() {
        Clan clan = createClan(TierCode.GOLD);
        UUID activeUserId = clan.getMembers().get(0).getUserId();
        UUID inactiveUserId = clan.getMembers().get(1).getUserId();

        List<ClanQuizScoreEvent> seasonEvents = List.of(
                createEvent(clan, activeUserId, 80.0d, 0.9d, 2),
                createEvent(clan, inactiveUserId, 40.0d, 0.9d, 8)
        );

        ClanScoreCalculator calculator = newCalculator(new DailyMissionStatusPort.PrimaryMissionCompletionSummary(false, 2, 0));
        CalculatedClanScore score = calculator.calculate(clan, seasonEvents);

        assertThat(score.baseScore()).isEqualTo((80.0d * 1.5d + 40.0d * 0.75d) / 2.25d);
    }

    @Test
    void diamondShouldApplyWeeklyActivityFactorWithCap() {
        Clan clan = createClan(TierCode.DIAMOND);
        UUID activeUserId = clan.getMembers().get(0).getUserId();
        UUID inactiveUserId = clan.getMembers().get(1).getUserId();

        List<ClanQuizScoreEvent> seasonEvents = List.of(
                createEvent(clan, activeUserId, 100.0d, 0.9d, 1),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 1),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 1),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 2),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 2),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 3),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 3),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 4),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 4),
                createEvent(clan, activeUserId, 100.0d, 0.9d, 5),
                createEvent(clan, inactiveUserId, 50.0d, 0.9d, 9)
        );

        ClanScoreCalculator calculator = newCalculator(new DailyMissionStatusPort.PrimaryMissionCompletionSummary(false, 2, 0));
        CalculatedClanScore score = calculator.calculate(clan, seasonEvents);

        double goldEquivalent = (1000.0d * 1.5d + 50.0d * 0.75d) / 2.25d;
        assertThat(score.baseScore()).isEqualTo(goldEquivalent * 1.25d);
    }

    @Test
    void modifiersShouldStackWhenConditionsAreMet() {
        Clan clan = createClan(TierCode.BRONZE);
        List<ClanQuizScoreEvent> seasonEvents = List.of(
                createEvent(clan, clan.getMembers().get(0).getUserId(), 60.0d, 0.4d, 1),
                createEvent(clan, clan.getMembers().get(1).getUserId(), 40.0d, 0.4d, 1)
        );

        ClanScoreCalculator calculator = newCalculator(new DailyMissionStatusPort.PrimaryMissionCompletionSummary(true, 2, 1));
        CalculatedClanScore score = calculator.calculate(clan, seasonEvents);

        assertThat(score.baseScore()).isEqualTo(100.0d);
        assertThat(score.finalScore()).isEqualTo(96.0d);
        assertThat(score.activeModifiers()).hasSize(2);
        assertThat(score.activeModifiers().stream().map(ActiveScoreModifier::code))
                .containsExactlyInAnyOrder("PRODUCTIVITY_BUFF", "LOW_ACCURACY_PENALTY");
    }

    private ClanScoreCalculator newCalculator(DailyMissionStatusPort.PrimaryMissionCompletionSummary summary) {
        DailyMissionStatusPort dailyMissionStatusPort = new DailyMissionStatusPort() {
            @Override
            public PrimaryMissionCompletionSummary summarizePrimaryMissionCompletion(
                    java.util.Collection<UUID> userIds,
                    LocalDate date
            ) {
                return summary;
            }
        };
        return new ClanScoreCalculator(
                dailyMissionStatusPort,
                List.of(
                        new BronzeTierScoreStrategy(),
                        new SilverTierScoreStrategy(),
                        new GoldTierScoreStrategy(),
                        new DiamondTierScoreStrategy()
                )
        );
    }

    private Clan createClan(TierCode tierCode) {
        Clan clan = new Clan("Clan-" + tierCode.name(), new Tier(tierCode, tierCode.name()), UUID.randomUUID());
        clan.addMember(new ClanMember(clan, UUID.randomUUID(), ClanMemberRole.LEADER));
        clan.addMember(new ClanMember(clan, UUID.randomUUID(), ClanMemberRole.MEMBER));
        return clan;
    }

    private ClanQuizScoreEvent createEvent(
            Clan clan,
            UUID userId,
            double score,
            double accuracy,
            long daysAgo
    ) {
        return new ClanQuizScoreEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                score,
                accuracy,
                LocalDateTime.now().minusDays(daysAgo)
        );
    }
}
