package id.ac.ui.cs.advprog.yomu.league.service;

import id.ac.ui.cs.advprog.yomu.integration.quiz.QuizCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LeagueQuizCompletionEventListener {

    private final ClanService clanService;

    public LeagueQuizCompletionEventListener(ClanService clanService) {
        this.clanService = clanService;
    }

    @EventListener
    public void handleQuizCompleted(QuizCompletedEvent event) {
        clanService.recordQuizCompletion(new ClanService.QuizCompletionPayload(
                event.eventId(),
                event.userId(),
                event.textId(),
                event.score(),
                event.accuracy(),
                event.completedAt()
        ));
    }
}
