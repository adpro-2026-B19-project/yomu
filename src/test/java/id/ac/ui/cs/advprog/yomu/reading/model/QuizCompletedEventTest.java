package id.ac.ui.cs.advprog.yomu.reading.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class QuizCompletedEventTest {

    @Test
    void constructorShouldExposeQuizCompletionData() {
        Object source = new Object();
        Instant completedAt = Instant.parse("2026-05-07T10:15:30Z");

        QuizCompletedEvent event = new QuizCompletedEvent(
                source,
                "user-1",
                12L,
                80.0,
                0.8,
                completedAt
        );

        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getUserId()).isEqualTo("user-1");
        assertThat(event.getTextId()).isEqualTo(12L);
        assertThat(event.getScore()).isEqualTo(80.0);
        assertThat(event.getAccuracy()).isEqualTo(0.8);
        assertThat(event.getCompletedAt()).isEqualTo(completedAt);
    }
}
