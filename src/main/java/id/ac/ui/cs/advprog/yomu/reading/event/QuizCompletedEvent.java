package id.ac.ui.cs.advprog.yomu.reading.event;

import java.util.UUID;

public record QuizCompletedEvent(
        UUID userId,
        Long textId,
        int score,
        int totalQuestions
) {}
