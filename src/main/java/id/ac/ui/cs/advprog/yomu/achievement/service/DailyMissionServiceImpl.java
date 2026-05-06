package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserMissionProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyMissionServiceImpl implements DailyMissionService {

    private final DailyMissionRepository dailyMissionRepository;
    private final UserMissionProgressRepository progressRepository;

    @Override
    @Transactional
    public void incrementProgress(UUID userId) {
        LocalDate today = LocalDate.now();
        List<DailyMission> todayMissions = dailyMissionRepository.findByActiveDate(today);

        for (DailyMission mission : todayMissions) {
            UserMissionProgress progress = progressRepository
                    .findByUserIdAndMissionId(userId, mission.getId())
                    .orElse(UserMissionProgress.builder()
                            .userId(userId)
                            .mission(mission)
                            .currentProgress(0)
                            .completed(false)
                            .build());

            if (!progress.isCompleted()) {
                progress.setCurrentProgress(progress.getCurrentProgress() + 1);
                if (progress.getCurrentProgress() >= mission.getTargetCount()) {
                    progress.setCompleted(true);
                }
                progressRepository.save(progress);
            }
        }
    }

    @Override
    public List<DailyMission> getTodayMissions() {
        return dailyMissionRepository.findByActiveDate(LocalDate.now());
    }

    @Override
    public List<UserMissionProgress> getUserProgress(UUID userId) {
        return progressRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public DailyMission createDailyMission(String title, int targetCount, boolean primary) {
        validateDailyMission(targetCount);
        if (primary) {
            clearExistingPrimaryMission(LocalDate.now(), null);
        }
        DailyMission mission = DailyMission.builder()
                .title(title)
                .targetCount(targetCount)
                .activeDate(LocalDate.now())
                .primary(primary)
                .build();
        return dailyMissionRepository.save(mission);
    }

    @Override
    @Transactional
    public DailyMission updateDailyMission(Long id, String title, int targetCount, boolean primary) {
        validateDailyMission(targetCount);
        DailyMission mission = dailyMissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Misi Harian dengan ID " + id + " tidak ditemukan"));
        if (primary) {
            clearExistingPrimaryMission(mission.getActiveDate(), id);
        }
        mission.setTitle(title);
        mission.setTargetCount(targetCount);
        mission.setPrimary(primary);
        return dailyMissionRepository.save(mission);
    }

    @Override
    @Transactional
    public void deleteDailyMission(Long id) {
        DailyMission mission = dailyMissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Misi Harian dengan ID " + id + " tidak ditemukan"));
        progressRepository.deleteByMissionId(id);
        dailyMissionRepository.delete(mission);
    }

    private void validateDailyMission(int targetCount) {
        if (targetCount <= 0) {
            throw new IllegalArgumentException("Target misi harian harus lebih besar dari nol");
        }
    }

    private void clearExistingPrimaryMission(LocalDate activeDate, Long excludedMissionId) {
        dailyMissionRepository.findByActiveDateAndPrimaryTrue(activeDate)
                .filter(existing -> excludedMissionId == null || !existing.getId().equals(excludedMissionId))
                .ifPresent(existing -> {
                    existing.setPrimary(false);
                    dailyMissionRepository.save(existing);
                });
    }
}
