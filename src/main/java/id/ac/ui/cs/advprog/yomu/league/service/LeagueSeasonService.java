package id.ac.ui.cs.advprog.yomu.league.service;

import id.ac.ui.cs.advprog.yomu.league.model.LeagueSeason;
import id.ac.ui.cs.advprog.yomu.league.repository.LeagueSeasonRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueSeasonService {

    private static final int MAX_CREATE_ATTEMPTS = 3;

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
        LeagueSeason activeSeason = leagueSeasonRepository.findByActiveTrue().orElse(null);
        if (activeSeason != null) {
            return activeSeason;
        }
        return createOrRefetchActiveSeason();
    }

    @Transactional
    public LeagueSeason endActiveSeason() {
        LeagueSeason activeSeason = getOrCreateActiveSeason();
        activeSeason.end();
        return leagueSeasonRepository.save(activeSeason);
    }

    @Transactional
    public LeagueSeason startNextSeason() {
        LeagueSeason activeSeason = leagueSeasonRepository.findByActiveTrue().orElse(null);
        if (activeSeason != null) {
            return activeSeason;
        }
        return createOrRefetchActiveSeason();
    }

    private LeagueSeason createNextSeason() {
        int nextSeasonNumber = leagueSeasonRepository.findTopByOrderBySeasonNumberDesc()
                .map(LeagueSeason::getSeasonNumber)
                .orElse(0) + 1;
        return leagueSeasonRepository.saveAndFlush(new LeagueSeason(nextSeasonNumber));
    }

    private LeagueSeason createOrRefetchActiveSeason() {
        for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
            try {
                return createNextSeason();
            } catch (DataIntegrityViolationException ignoredConflict) {
                LeagueSeason activeSeason = leagueSeasonRepository.findByActiveTrue().orElse(null);
                if (activeSeason != null) {
                    return activeSeason;
                }
            }
        }
        throw new IllegalStateException("Unable to create or resolve an active season");
    }
}
