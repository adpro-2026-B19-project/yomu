package id.ac.ui.cs.advprog.yomu.league.scoring;

import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import org.springframework.stereotype.Component;

@Component
public class SilverTierScoreStrategy implements TierScoreStrategy {

    @Override
    public TierCode supportedTier() {
        return TierCode.SILVER;
    }

    @Override
    public double calculateBaseScore(ClanScoreSnapshot snapshot) {
        return snapshot.weightedAverage(1.25d, 1.0d);
    }

    @Override
    public String formulaDescription() {
        return "Silver: rata-rata tertimbang total skor season per anggota, bobot aktif 1.25 dan nonaktif 1.0.";
    }
}
