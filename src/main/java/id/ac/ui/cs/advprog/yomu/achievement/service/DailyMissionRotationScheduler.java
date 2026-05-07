package id.ac.ui.cs.advprog.yomu.achievement.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMissionRotationScheduler {

    private final DailyMissionService dailyMissionService;

    @Scheduled(cron = "0 0 0 * * *")
    public void rotateDailyMissions() {
        int rotatedMissionCount = dailyMissionService.rotateDailyMissions(LocalDate.now()).size();
        log.info("Daily mission rotation completed for {} with {} mission(s)", LocalDate.now(), rotatedMissionCount);
    }
}
