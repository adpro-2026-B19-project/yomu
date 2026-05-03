package id.ac.ui.cs.advprog.yomu.reading.model;

import org.springframework.context.ApplicationEvent;
import lombok.Getter;
import java.time.Instant;

@Getter
public class QuizCompletedEvent extends ApplicationEvent {

    private final String userId;
    private final Long textId;
    private final Double score;
    private final Double accuracy;
    private final Instant completedAt;

    public QuizCompletedEvent(Object source, String userId, Long textId, Double score, Double accuracy, Instant completedAt) {
        super(source);
        this.userId = userId;
        this.textId = textId;
        this.score = score;
        this.accuracy = accuracy;
        this.completedAt = completedAt;
    }
}
