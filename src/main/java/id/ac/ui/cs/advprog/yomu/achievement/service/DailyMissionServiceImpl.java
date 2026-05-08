package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.model.UserMissionProgress;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserMissionProgressRepository;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
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
    private final CategoryRepository categoryRepository;
    private final TextRepository textRepository;

    @Override
    @Transactional
    public void incrementProgress(UUID userId, Long readingTextId) {
        LocalDate today = LocalDate.now();
        List<DailyMission> todayMissions = dailyMissionRepository.findByActiveDate(today);
        Long completedCategoryId = resolveCategoryId(readingTextId);

        for (DailyMission mission : todayMissions) {
            if (mission.getCategoryId() != null && !mission.getCategoryId().equals(completedCategoryId)) {
                continue;
            }

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
    public DailyMission createDailyMission(String title, int targetCount, boolean primary, Long categoryId) {
        String normalizedTitle = validateAndNormalizeTitle(title);
        validateDailyMission(targetCount);
        Long normalizedCategoryId = validateAndNormalizeCategoryId(categoryId);
        if (primary) {
            clearExistingPrimaryMission(LocalDate.now(), null);
        }
        DailyMission mission = DailyMission.builder()
                .title(normalizedTitle)
                .targetCount(targetCount)
                .activeDate(LocalDate.now())
                .primary(primary)
                .categoryId(normalizedCategoryId)
                .build();
        return dailyMissionRepository.save(mission);
    }

    @Override
    @Transactional
    public DailyMission updateDailyMission(Long id, String title, int targetCount, boolean primary, Long categoryId) {
        String normalizedTitle = validateAndNormalizeTitle(title);
        validateDailyMission(targetCount);
        Long normalizedCategoryId = validateAndNormalizeCategoryId(categoryId);
        DailyMission mission = dailyMissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Misi Harian dengan ID " + id + " tidak ditemukan"));
        if (primary) {
            clearExistingPrimaryMission(mission.getActiveDate(), id);
        }
        mission.setTitle(normalizedTitle);
        mission.setTargetCount(targetCount);
        mission.setPrimary(primary);
        mission.setCategoryId(normalizedCategoryId);
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

    @Override
    @Transactional
    public List<DailyMission> rotateDailyMissions(LocalDate targetDate) {
        List<DailyMission> existingMissions = dailyMissionRepository.findByActiveDate(targetDate);
        if (!existingMissions.isEmpty()) {
            return existingMissions;
        }

        return dailyMissionRepository.findFirstByActiveDateBeforeOrderByActiveDateDesc(targetDate)
                .map(latestMission -> dailyMissionRepository.findByActiveDate(latestMission.getActiveDate()).stream()
                        .map(mission -> DailyMission.builder()
                                .title(mission.getTitle())
                                .targetCount(mission.getTargetCount())
                                .activeDate(targetDate)
                                .primary(mission.isPrimary())
                                .categoryId(mission.getCategoryId())
                                .build())
                        .map(dailyMissionRepository::save)
                        .toList())
                .orElse(List.of());
    }

    private void validateDailyMission(int targetCount) {
        if (targetCount <= 0) {
            throw new IllegalArgumentException("Target misi harian harus lebih besar dari nol");
        }
    }

    private String validateAndNormalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Judul misi harian wajib diisi");
        }
        return title.trim();
    }

    private Long validateAndNormalizeCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Kategori misi harian tidak ditemukan"));
        return categoryId;
    }

    private Long resolveCategoryId(Long readingTextId) {
        if (readingTextId == null) {
            return null;
        }
        Text text = textRepository.findById(readingTextId).orElse(null);
        if (text == null || text.getCategory() == null) {
            return null;
        }
        return text.getCategory().getId();
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
