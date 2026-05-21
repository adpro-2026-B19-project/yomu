package id.ac.ui.cs.advprog.yomu.config;

import static org.assertj.core.api.Assertions.assertThat;

import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.demo.seed.enabled=true")
class DemoSeedIntegrationTest {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private ClanRepository clanRepository;

    @Test
    void demoSeedShouldProvisionPresentationReadyData() {
        assertThat(authRepository.findByEmail("hasanul.muttaqin@ui.ac.id")).isPresent();
        assertThat(authRepository.findByEmail("kalfin.demo.leader@yomu.test")).isPresent();
        assertThat(authRepository.findByEmail("kalfin.demo.member@yomu.test")).isPresent();
        assertThat(dailyMissionRepository.findByActiveDateAndPrimaryTrue(LocalDate.now())).isPresent();
        assertThat(clanRepository.findAllForListing())
                .extracting(clan -> clan.getName())
                .contains("Kalfin Demo Clan");
    }
}
