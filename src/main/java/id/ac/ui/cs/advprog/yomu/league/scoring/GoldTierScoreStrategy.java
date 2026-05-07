package id.ac.ui.cs.advprog.yomu.league.scoring;

import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import org.springframework.stereotype.Component;

@Component
public class GoldTierScoreStrategy implements TierScoreStrategy {

    @Override
    public TierCode supportedTier() {
        return TierCode.GOLD;
    }

    @Override
    public double calculateBaseScore(ClanScoreSnapshot snapshot) {
        return snapshot.weightedAverage(1.5d, 0.75d);
    }

    @Override
    public String formulaDescription() {
        return "Gold: rata-rata tertimbang total skor season per anggota, bobot aktif 1.5 dan nonaktif 0.75.";
    }
}
