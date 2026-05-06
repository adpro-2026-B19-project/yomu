package id.ac.ui.cs.advprog.yomu.league.scoring;

import id.ac.ui.cs.advprog.yomu.league.model.TierCode;

public interface TierScoreStrategy {
    TierCode supportedTier();

    double calculateBaseScore(ClanScoreSnapshot snapshot);

    String formulaDescription();
}
