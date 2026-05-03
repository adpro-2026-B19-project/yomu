package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserStatistic;
import id.ac.ui.cs.advprog.yomu.achievement.repository.AchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserStatisticRepository userStatisticRepository;

    @Override
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    @Override
    public List<UserAchievement> getAchievementsByUserId(UUID userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Achievement createAchievement(String name, String milestone) {
        if (achievementRepository.existsByName(name)) {
            throw new IllegalArgumentException("Achievement dengan nama tersebut sudah ada");
        }
        Achievement achievement = Achievement.builder()
                .name(name)
                .milestone(milestone)
                .build();
        return achievementRepository.save(achievement);
    }

    @Override
    @Transactional
    public void toggleDisplayAchievement(UUID userId, Long achievementId) {
        Optional<UserAchievement> optionalUa = userAchievementRepository.findByUserId(userId)
                .stream()
                .filter(ua -> ua.getAchievement().getId().equals(achievementId))
                .findFirst();

        if (optionalUa.isPresent()) {
            UserAchievement ua = optionalUa.get();
            ua.setDisplayed(!ua.isDisplayed());
            userAchievementRepository.save(ua);
        } else {
            throw new IllegalArgumentException("User belum membuka achievement ini");
        }
    }

    @Override
    @Transactional
    public void processQuizCompletion(UUID userId, LocalDateTime completedAt) {
        UserStatistic stat = userStatisticRepository.findByUserId(userId)
                .orElse(UserStatistic.builder().userId(userId).totalReadings(0).build());

        stat.setTotalReadings(stat.getTotalReadings() + 1);
        userStatisticRepository.save(stat);

        int totalUserReading = stat.getTotalReadings();

        List<Achievement> allAchievements = achievementRepository.findAll();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);
        List<Achievement> unobtainedAchievements = allAchievements.stream()
                .filter(ach -> userAchievements.stream()
                        .noneMatch(ua -> ua.getAchievement().getId().equals(ach.getId())))
                .toList();

        for (Achievement ach : unobtainedAchievements) {
            try {
                int targetMilestone = Integer.parseInt(ach.getMilestone());
                if (totalUserReading >= targetMilestone) {
                    UserAchievement newUa = UserAchievement.builder()
                            .userId(userId)
                            .achievement(ach)
                            .displayed(false)
                            .unlockedAt(completedAt)
                            .build();
                    userAchievementRepository.save(newUa);
                }
            } catch (NumberFormatException e) {}
        }
    }
}
