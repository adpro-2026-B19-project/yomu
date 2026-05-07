package id.ac.ui.cs.advprog.yomu.league.scoring;

import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import org.springframework.stereotype.Component;

@Component
public class BronzeTierScoreStrategy implements TierScoreStrategy {

    @Override
    public TierCode supportedTier() {
        return TierCode.BRONZE;
    }

    @Override
    public double calculateBaseScore(ClanScoreSnapshot snapshot) {
        return snapshot.totalSeasonScore();
    }

    @Override
    public String formulaDescription() {
        return "Bronze: total skor kuis semua anggota pada season aktif.";
    }
}
