package id.ac.ui.cs.advprog.yomu.league.service;

import id.ac.ui.cs.advprog.yomu.league.model.LeagueSeason;
import id.ac.ui.cs.advprog.yomu.league.repository.LeagueSeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueSeasonService {

    private final LeagueSeasonRepository leagueSeasonRepository;

    public LeagueSeasonService(LeagueSeasonRepository leagueSeasonRepository) {
        this.leagueSeasonRepository = leagueSeasonRepository;
    }

    @Transactional(readOnly = true)
    public LeagueSeason findActiveSeason() {
        return leagueSeasonRepository.findByActiveTrue().orElse(null);
    }

    @Transactional
    public LeagueSeason getOrCreateActiveSeason() {
        return leagueSeasonRepository.findByActiveTrue()
                .orElseGet(this::createNextSeason);
    }

    @Transactional
    public LeagueSeason endActiveSeason() {
        LeagueSeason activeSeason = getOrCreateActiveSeason();
        activeSeason.end();
        return leagueSeasonRepository.save(activeSeason);
    }

    @Transactional
    public LeagueSeason startNextSeason() {
        leagueSeasonRepository.findByActiveTrue().ifPresent(existing -> {
            throw new IllegalStateException("Cannot start a new season while another season is still active");
        });
        return createNextSeason();
    }

    private LeagueSeason createNextSeason() {
        int nextSeasonNumber = leagueSeasonRepository.findTopByOrderBySeasonNumberDesc()
                .map(LeagueSeason::getSeasonNumber)
                .orElse(0) + 1;
        return leagueSeasonRepository.save(new LeagueSeason(nextSeasonNumber));
    }
}
