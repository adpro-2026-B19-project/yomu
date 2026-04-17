package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AchievementQuizCompletionEventListener {

    private final AchievementService achievementService;

    public AchievementQuizCompletionEventListener(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @EventListener
    public void handleQuizCompleted(QuizCompletedEvent event) {
        achievementService.processQuizCompletion(event.userId(), event.completedAt());
    }
}
