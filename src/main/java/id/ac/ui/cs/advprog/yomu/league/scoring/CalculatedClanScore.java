package id.ac.ui.cs.advprog.yomu.league.scoring;

import java.util.List;

public record CalculatedClanScore(
        double baseScore,
        double finalScore,
        List<ActiveScoreModifier> activeModifiers,
        String formulaDescription
) {
}
