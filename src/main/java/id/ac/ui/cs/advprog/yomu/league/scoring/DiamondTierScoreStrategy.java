package id.ac.ui.cs.advprog.yomu.league.scoring;

import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import org.springframework.stereotype.Component;

@Component
public class DiamondTierScoreStrategy implements TierScoreStrategy {

    @Override
    public TierCode supportedTier() {
        return TierCode.DIAMOND;
    }

    @Override
    public double calculateBaseScore(ClanScoreSnapshot snapshot) {
        double goldEquivalentScore = snapshot.weightedAverage(1.5d, 0.75d);
        double weeklyFactor = 1.0d + Math.min(0.25d, 0.05d * snapshot.averageWeeklyCompletionsPerActiveMember());
        return goldEquivalentScore * weeklyFactor;
    }

    @Override
    public String formulaDescription() {
        return "Diamond: formula Gold dikali faktor aktivitas mingguan dengan cap bonus 25%.";
    }
}
