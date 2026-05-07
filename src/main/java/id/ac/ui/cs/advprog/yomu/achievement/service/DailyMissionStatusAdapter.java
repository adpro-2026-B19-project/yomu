package id.ac.ui.cs.advprog.yomu.achievement.service;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.achievement.repository.UserMissionProgressRepository;
import id.ac.ui.cs.advprog.yomu.integration.dailymission.DailyMissionStatusPort;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DailyMissionStatusAdapter implements DailyMissionStatusPort {

    private final DailyMissionRepository dailyMissionRepository;
    private final UserMissionProgressRepository userMissionProgressRepository;

    public DailyMissionStatusAdapter(
            DailyMissionRepository dailyMissionRepository,
            UserMissionProgressRepository userMissionProgressRepository
    ) {
        this.dailyMissionRepository = dailyMissionRepository;
        this.userMissionProgressRepository = userMissionProgressRepository;
    }

    @Override
    public PrimaryMissionCompletionSummary summarizePrimaryMissionCompletion(Collection<UUID> userIds, LocalDate date) {
        if (userIds == null || userIds.isEmpty()) {
            return new PrimaryMissionCompletionSummary(false, 0, 0);
        }

        DailyMission primaryMission = dailyMissionRepository.findByActiveDateAndPrimaryTrue(date).orElse(null);
        if (primaryMission == null) {
            return new PrimaryMissionCompletionSummary(false, userIds.size(), 0);
        }

        long completedUsers = userMissionProgressRepository
                .countByMission_IdAndUserIdInAndCompletedTrue(primaryMission.getId(), userIds);
        return new PrimaryMissionCompletionSummary(true, userIds.size(), completedUsers);
    }
}
