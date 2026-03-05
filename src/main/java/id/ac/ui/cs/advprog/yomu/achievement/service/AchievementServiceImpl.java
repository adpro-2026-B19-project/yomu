package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserAchievement;
import id.ac.ui.cs.advprog.yomu.achievement.repository.AchievementRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserAchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

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
}