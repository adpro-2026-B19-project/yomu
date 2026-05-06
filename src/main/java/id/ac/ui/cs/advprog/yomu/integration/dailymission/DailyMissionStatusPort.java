package id.ac.ui.cs.advprog.yomu.integration.dailymission;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

public interface DailyMissionStatusPort {
    PrimaryMissionCompletionSummary summarizePrimaryMissionCompletion(Collection<UUID> userIds, LocalDate date);

    record PrimaryMissionCompletionSummary(
            boolean primaryMissionExists,
            long totalUsers,
            long completedUsers
    ) {
    }
}
