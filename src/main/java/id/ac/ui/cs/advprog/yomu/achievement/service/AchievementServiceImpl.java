package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.Achievement;
import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
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
    public List<AchievementDistribution> getAchievementDistribution() {
        return achievementRepository.findAll().stream()
                .map(achievement -> new AchievementDistribution(
                        achievement.getId(),
                        achievement.getName(),
                        achievement.getMilestone(),
                        userAchievementRepository.countByAchievement_Id(achievement.getId())
                ))
                .toList();
    }

    @Override
    public List<AchievementProgress> getAchievementProgress(UUID userId) {
        UserStatistic statistic = userStatisticRepository.findByUserId(userId)
                .orElse(UserStatistic.builder().userId(userId).totalReadings(0).totalScore(0.0d).build());
        List<UserAchievement> unlockedAchievements = userAchievementRepository.findByUserId(userId);

        return achievementRepository.findAll().stream()
                .map(achievement -> {
                    Optional<UserAchievement> unlocked = unlockedAchievements.stream()
                            .filter(userAchievement -> userAchievement.getAchievement().getId().equals(achievement.getId()))
                            .findFirst();
                    double currentValue = getCurrentValue(achievement.getRequirementType(), statistic);
                    int progressPercent = achievement.getTargetValue() <= 0
                            ? 0
                            : (int) Math.min(100, Math.floor((currentValue / achievement.getTargetValue()) * 100));
                    return new AchievementProgress(
                            achievement.getId(),
                            achievement.getName(),
                            achievement.getMilestone(),
                            achievement.getRequirementType(),
                            achievement.getTargetValue(),
                            currentValue,
                            progressPercent,
                            unlocked.isPresent(),
                            unlocked.map(UserAchievement::isDisplayed).orElse(false)
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public Achievement createAchievement(
            String name,
            String milestone,
            AchievementRequirementType requirementType,
            int targetValue
    ) {
        String normalizedName = normalizeRequiredText(name, "Nama achievement wajib diisi");
        if (achievementRepository.existsByName(normalizedName)) {
            throw new IllegalArgumentException("Achievement dengan nama tersebut sudah ada");
        }
        return achievementRepository.save(buildAchievement(null, normalizedName, milestone, requirementType, targetValue));
    }

    @Override
    @Transactional
    public Achievement updateAchievement(
            Long id,
            String name,
            String milestone,
            AchievementRequirementType requirementType,
            int targetValue
    ) {
        Achievement existing = achievementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Achievement tidak ditemukan"));
        String normalizedName = normalizeRequiredText(name, "Nama achievement wajib diisi");
        achievementRepository.findByName(normalizedName)
                .filter(candidate -> !candidate.getId().equals(id))
                .ifPresent(candidate -> {
                    throw new IllegalArgumentException("Achievement dengan nama tersebut sudah ada");
                });
        existing.setName(normalizedName);
        existing.setMilestone(milestone);
        existing.setRequirementType(normalizeRequirementType(requirementType));
        existing.setTargetValue(normalizeTargetValue(targetValue));
        return achievementRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteAchievement(Long id) {
        if (!achievementRepository.existsById(id)) {
            throw new IllegalArgumentException("Achievement tidak ditemukan");
        }
        userAchievementRepository.deleteByAchievementId(id);
        achievementRepository.deleteById(id);
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
    public void processQuizCompletion(UUID userId, double score, LocalDateTime completedAt) {
        UserStatistic stat = userStatisticRepository.findByUserId(userId)
                .orElse(UserStatistic.builder().userId(userId).totalReadings(0).totalScore(0.0d).build());

        stat.setTotalReadings(stat.getTotalReadings() + 1);
        stat.setTotalScore(stat.getTotalScore() + Math.max(0.0d, score));
        userStatisticRepository.save(stat);

        int totalUserReading = stat.getTotalReadings();
        double totalUserScore = stat.getTotalScore();

        List<Achievement> allAchievements = achievementRepository.findAll();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);
        List<Achievement> unobtainedAchievements = allAchievements.stream()
                .filter(ach -> userAchievements.stream()
                        .noneMatch(ua -> ua.getAchievement().getId().equals(ach.getId())))
                .toList();

        for (Achievement ach : unobtainedAchievements) {
            boolean unlockedByReading = ach.getRequirementType() == AchievementRequirementType.READING_COUNT
                    && totalUserReading >= ach.getTargetValue();
            boolean unlockedByScore = ach.getRequirementType() == AchievementRequirementType.TOTAL_SCORE
                    && totalUserScore >= ach.getTargetValue();
            if ((unlockedByReading || unlockedByScore)
                    && !userAchievementRepository.existsByUserIdAndAchievementId(userId, ach.getId())) {
                UserAchievement newUa = UserAchievement.builder()
                        .userId(userId)
                        .achievement(ach)
                        .displayed(false)
                        .unlockedAt(completedAt)
                        .build();
                userAchievementRepository.save(newUa);
            }
        }
    }

    private Achievement buildAchievement(
            Long id,
            String name,
            String milestone,
            AchievementRequirementType requirementType,
            int targetValue
    ) {
        String normalizedName = normalizeRequiredText(name, "Nama achievement wajib diisi");
        String normalizedMilestone = normalizeRequiredText(milestone, "Deskripsi milestone wajib diisi");
        return Achievement.builder()
                .id(id)
                .name(normalizedName)
                .milestone(normalizedMilestone)
                .requirementType(normalizeRequirementType(requirementType))
                .targetValue(normalizeTargetValue(targetValue))
                .build();
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private AchievementRequirementType normalizeRequirementType(AchievementRequirementType requirementType) {
        if (requirementType == null) {
            return AchievementRequirementType.READING_COUNT;
        }
        return requirementType;
    }

    private int normalizeTargetValue(int targetValue) {
        if (targetValue <= 0) {
            throw new IllegalArgumentException("Target achievement harus lebih besar dari nol");
        }
        return targetValue;
    }

    private double getCurrentValue(AchievementRequirementType requirementType, UserStatistic statistic) {
        return switch (requirementType) {
            case TOTAL_SCORE -> statistic.getTotalScore();
            case READING_COUNT -> statistic.getTotalReadings();
        };
    }
}
