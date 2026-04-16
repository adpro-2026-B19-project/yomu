package id.ac.ui.cs.advprog.yomu.integration.quiz;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuizCompletedEvent(
        UUID eventId,
        UUID userId,
        UUID textId,
        double score,
        double accuracy,
        LocalDateTime completedAt
) {
}
