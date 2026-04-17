package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.repository.AchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private static final Pattern FIRST_NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Override
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    @Override
    public Achievement createAchievement(String name, String milestone) {
        if (achievementRepository.existsByName(name)) {
            throw new IllegalArgumentException("Achievement with name '" + name + "' already exists");
        }
        Achievement achievement = Achievement.builder()
                .name(name)
                .milestone(milestone)
                .build();
        return achievementRepository.save(achievement);
    }

    @Override
    public List<UserAchievement> getAchievementsByUserId(UUID userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    @Override
    public void processQuizCompletion(UUID userId, LocalDateTime completedAt) {
        if (userId == null || completedAt == null) {
            return;
        }

        long completedQuizCount = quizAttemptRepository.countByUserId(userId.toString());
        for (Achievement achievement : achievementRepository.findAll()) {
            int requiredQuizCount = extractRequiredQuizCount(achievement.getMilestone());
            if (completedQuizCount < requiredQuizCount) {
                continue;
            }
            if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
                continue;
            }

            UserAchievement unlockedAchievement = UserAchievement.builder()
                    .userId(userId)
                    .achievement(achievement)
                    .unlockedAt(completedAt)
                    .displayed(false)
                    .build();
            userAchievementRepository.save(unlockedAchievement);
        }
    }

    private int extractRequiredQuizCount(String milestone) {
        if (milestone == null || milestone.isBlank()) {
            return 1;
        }

        Matcher matcher = FIRST_NUMBER_PATTERN.matcher(milestone);
        if (!matcher.find()) {
            return 1;
        }
        return Math.max(1, Integer.parseInt(matcher.group(1)));
    }
}
