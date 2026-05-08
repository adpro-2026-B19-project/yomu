package id.ac.ui.cs.advprog.yomu.league.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeagueQuizCompletionEventListenerTest {

    @Mock
    private ClanService clanService;

    @Test
    void handleQuizCompletedShouldForwardPayloadToClanService() {
        LeagueQuizCompletionEventListener listener = new LeagueQuizCompletionEventListener(clanService);
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID textId = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.now().minusMinutes(1);

        listener.handleQuizCompleted(new QuizCompletedEvent(
                eventId,
                userId,
                textId,
                12L,
                8.5d,
                0.85d,
                completedAt
        ));

        verify(clanService).recordQuizCompletion(argThat(payload ->
                payload.eventId().equals(eventId)
                        && payload.userId().equals(userId)
                        && payload.textId().equals(textId)
                        && payload.score() == 8.5d
                        && payload.accuracy() == 0.85d
                        && payload.completedAt().equals(completedAt)
        ));
    }
}
