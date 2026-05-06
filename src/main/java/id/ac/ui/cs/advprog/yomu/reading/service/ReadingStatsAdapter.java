package id.ac.ui.cs.advprog.yomu.reading.service;

import id.ac.ui.cs.advprog.yomu.integration.reading.ReadingStatsPort;
import id.ac.ui.cs.advprog.yomu.reading.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ReadingStatsAdapter implements ReadingStatsPort {

    private final QuizAttemptRepository quizAttemptRepository;

    public ReadingStatsAdapter(QuizAttemptRepository quizAttemptRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
    }

    @Override
    public UserReadingStats getUserReadingStats(UUID userId) {
        if (userId == null) {
            return new UserReadingStats(0, 0.0d, 0.0d);
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId.toString());
        if (attempts.isEmpty()) {
            return new UserReadingStats(0, 0.0d, 0.0d);
        }

        double totalAccuracy = 0.0d;
        double totalScore = 0.0d;
        for (QuizAttempt attempt : attempts) {
            totalAccuracy += attempt.getAccuracy();
            totalScore += attempt.getScore();
        }

        return new UserReadingStats(
                attempts.size(),
                totalAccuracy / attempts.size(),
                totalScore
        );
    }
}
