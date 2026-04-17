package id.ac.ui.cs.advprog.yomu.achievement.service;

import static org.mockito.Mockito.verify;

import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AchievementQuizCompletionEventListenerTest {

    @Mock
    private AchievementService achievementService;

    @Mock
    private DailyMissionService dailyMissionService;

    @Test
    void handleQuizCompletedShouldForwardToAchievementServiceAndDailyMissionService() {
        AchievementQuizCompletionEventListener listener = new AchievementQuizCompletionEventListener(achievementService, dailyMissionService);
        UUID userId = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.now().minusMinutes(1);

        listener.handleQuizCompleted(new QuizCompletedEvent(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                100.0d,
                1.0d,
                completedAt
        ));
        verify(dailyMissionService).incrementProgress(userId);
        verify(achievementService).processQuizCompletion(userId, completedAt);
    }
}
