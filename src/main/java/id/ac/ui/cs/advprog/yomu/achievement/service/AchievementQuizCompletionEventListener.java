package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementQuizCompletionEventListener {
    private final AchievementService achievementService;
    private final DailyMissionService dailyMissionService;

    @EventListener
    public void handleQuizCompleted(QuizCompletedEvent event) {
        dailyMissionService.incrementProgress(event.userId());
        achievementService.processQuizCompletion(event.userId(), event.completedAt());
    }
}
