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
        if (achievementRepository.findByName(name).isPresent()) {
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
        // 1. Ambil atau buat statistik user baru (Logika Modul Achievement Anda)
        UserStatistic stat = userStatisticRepository.findByUserId(userId)
                .orElse(UserStatistic.builder().userId(userId).totalReadings(0).build());

        // 2. Tambahkan hitungan total bacaan
        stat.setTotalReadings(stat.getTotalReadings() + 1);
        userStatisticRepository.save(stat);

        int totalUserReading = stat.getTotalReadings();

        // 3. Ambil data achievement
        List<Achievement> allAchievements = achievementRepository.findAll();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);

        // 4. Filter achievement yang BELUM dimiliki user
        List<Achievement> unobtainedAchievements = allAchievements.stream()
                .filter(ach -> userAchievements.stream()
                        .noneMatch(ua -> ua.getAchievement().getId().equals(ach.getId())))
                .toList();

        // 5. Cek kondisi unlock
        for (Achievement ach : unobtainedAchievements) {
            try {
                int targetMilestone = Integer.parseInt(ach.getMilestone());
                // Jika total bacaan saat ini mencapai atau melebihi target milestone
                if (totalUserReading >= targetMilestone) {
                    UserAchievement newUa = UserAchievement.builder()
                            .userId(userId)
                            .achievement(ach)
                            .displayed(false)
                            .build();
                    userAchievementRepository.save(newUa);
                }
            } catch (NumberFormatException e) {
                // Abaikan jika milestone bukan angka
            }
        }
    }
}
