package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.integration.reading.ReadingStatsPort.UserReadingStats;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingStatsAdapterTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private ReadingStatsAdapter readingStatsAdapter;

    @Test
    void getUserReadingStatsShouldReturnZeroForNullUserId() {
        UserReadingStats result = readingStatsAdapter.getUserReadingStats(null);

        assertThat(result.totalTextsCompleted()).isZero();
        assertThat(result.averageAccuracy()).isZero();
        assertThat(result.totalScore()).isZero();
    }

    @Test
    void getUserReadingStatsShouldReturnZeroWhenUserHasNoAttempts() {
        UUID userId = UUID.randomUUID();
        when(quizAttemptRepository.findByUserId(userId.toString())).thenReturn(List.of());

        UserReadingStats result = readingStatsAdapter.getUserReadingStats(userId);

        assertThat(result.totalTextsCompleted()).isZero();
        assertThat(result.averageAccuracy()).isZero();
        assertThat(result.totalScore()).isZero();
    }

    @Test
    void getUserReadingStatsShouldAggregateCompletedTextsAccuracyAndScore() {
        UUID userId = UUID.randomUUID();
        Text text = new Text();
        QuizAttempt first = new QuizAttempt(text, userId.toString(), 100.0, 1.0);
        QuizAttempt second = new QuizAttempt(text, userId.toString(), 40.0, 0.4);

        when(quizAttemptRepository.findByUserId(userId.toString())).thenReturn(List.of(first, second));

        UserReadingStats result = readingStatsAdapter.getUserReadingStats(userId);

        assertThat(result.totalTextsCompleted()).isEqualTo(2);
        assertThat(result.averageAccuracy()).isEqualTo(0.7);
        assertThat(result.totalScore()).isEqualTo(140.0);
    }
}
