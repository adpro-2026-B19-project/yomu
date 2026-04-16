package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.reading.event.QuizCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizEventListener {
    @EventListener
    public void handleQuizCompleted(QuizCompletedEvent event) {
        log.info("Menerima event kuis selesai untuk user: {}", event.userId());
    }
}
