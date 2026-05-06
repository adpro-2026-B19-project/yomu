package id.ac.ui.cs.advprog.yomu.league.scoring;

import id.ac.ui.cs.advprog.yomu.integration.dailymission.DailyMissionStatusPort;
import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanQuizScoreEvent;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ClanScoreCalculator {

    private static final double PRODUCTIVITY_BUFF_MULTIPLIER = 1.2d;
    private static final double LOW_ACCURACY_PENALTY_MULTIPLIER = 0.8d;
    private static final double LOW_ACCURACY_THRESHOLD = 0.5d;

    private final DailyMissionStatusPort dailyMissionStatusPort;
    private final Map<TierCode, TierScoreStrategy> strategiesByTier = new EnumMap<>(TierCode.class);

    public ClanScoreCalculator(
            DailyMissionStatusPort dailyMissionStatusPort,
            List<TierScoreStrategy> tierScoreStrategies
    ) {
        this.dailyMissionStatusPort = dailyMissionStatusPort;
        for (TierScoreStrategy strategy : tierScoreStrategies) {
            this.strategiesByTier.put(strategy.supportedTier(), strategy);
        }
    }

    public CalculatedClanScore calculate(Clan clan, List<ClanQuizScoreEvent> seasonEvents) {
        ClanScoreSnapshot snapshot = ClanScoreSnapshot.from(clan, seasonEvents);
        TierCode tierCode = clan.getTier().getCode();
        TierScoreStrategy strategy = strategiesByTier.get(tierCode);
        if (strategy == null) {
            throw new IllegalArgumentException("No score strategy registered for tier " + tierCode);
        }

        double baseScore = strategy.calculateBaseScore(snapshot);
        List<ActiveScoreModifier> activeModifiers = evaluateActiveModifiers(snapshot);
        double finalScore = baseScore;
        for (ActiveScoreModifier modifier : activeModifiers) {
            finalScore *= modifier.multiplier();
        }

        return new CalculatedClanScore(baseScore, finalScore, List.copyOf(activeModifiers), strategy.formulaDescription());
    }

    private List<ActiveScoreModifier> evaluateActiveModifiers(ClanScoreSnapshot snapshot) {
        List<ActiveScoreModifier> activeModifiers = new ArrayList<>();

        DailyMissionStatusPort.PrimaryMissionCompletionSummary primaryMissionSummary =
                dailyMissionStatusPort.summarizePrimaryMissionCompletion(
                        snapshot.currentMemberUserIds(),
                        LocalDate.now()
                );
        if (primaryMissionSummary.primaryMissionExists()
                && primaryMissionSummary.totalUsers() > 0
                && (double) primaryMissionSummary.completedUsers() / primaryMissionSummary.totalUsers() >= 0.5d) {
            activeModifiers.add(new ActiveScoreModifier(
                    "PRODUCTIVITY_BUFF",
                    "Productivity Buff",
                    PRODUCTIVITY_BUFF_MULTIPLIER,
                    "Aktif karena minimal 50% anggota menyelesaikan primary daily mission hari ini."
            ));
        }

        if (snapshot.hasSeasonActivity() && snapshot.averageAccuracy() < LOW_ACCURACY_THRESHOLD) {
            activeModifiers.add(new ActiveScoreModifier(
                    "LOW_ACCURACY_PENALTY",
                    "Low Accuracy Penalty",
                    LOW_ACCURACY_PENALTY_MULTIPLIER,
                    "Aktif karena rata-rata akurasi kuis clan di season aktif masih di bawah 50%."
            ));
        }

        return activeModifiers;
    }
}
