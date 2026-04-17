package id.ac.ui.cs.advprog.yomu.league.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record QuizCompletionApiEventRequest(
        @NotNull(message = "eventId is required")
        UUID eventId,

        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "textId is required")
        UUID textId,

        @NotNull(message = "score is required")
        Double score,

        @NotNull(message = "accuracy is required")
        Double accuracy,

        @NotNull(message = "completedAt is required")
        LocalDateTime completedAt
) {
}
