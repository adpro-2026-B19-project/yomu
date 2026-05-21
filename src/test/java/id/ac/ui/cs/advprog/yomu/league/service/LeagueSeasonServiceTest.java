package id.ac.ui.cs.advprog.yomu.league.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.league.model.LeagueSeason;
import id.ac.ui.cs.advprog.yomu.league.repository.LeagueSeasonRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class LeagueSeasonServiceTest {

    @Mock
    private LeagueSeasonRepository leagueSeasonRepository;

    @InjectMocks
    private LeagueSeasonService leagueSeasonService;

    @Test
    void getOrCreateActiveSeasonShouldReturnExistingSeasonImmediately() {
        LeagueSeason activeSeason = new LeagueSeason(4);
        when(leagueSeasonRepository.findByActiveTrue()).thenReturn(Optional.of(activeSeason));

        LeagueSeason result = leagueSeasonService.getOrCreateActiveSeason();

        assertThat(result).isSameAs(activeSeason);
        verify(leagueSeasonRepository, times(1)).findByActiveTrue();
    }

    @Test
    void getOrCreateActiveSeasonShouldRefetchAfterConcurrentCreationConflict() {
        LeagueSeason activeSeason = new LeagueSeason(1);

        when(leagueSeasonRepository.findByActiveTrue())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(activeSeason));
        when(leagueSeasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.empty());
        when(leagueSeasonRepository.saveAndFlush(any(LeagueSeason.class)))
                .thenThrow(new DataIntegrityViolationException("season already created"));

        LeagueSeason result = leagueSeasonService.getOrCreateActiveSeason();

        assertThat(result).isSameAs(activeSeason);
        verify(leagueSeasonRepository).saveAndFlush(any(LeagueSeason.class));
    }

    @Test
    void startNextSeasonShouldRefetchActiveSeasonAfterConflict() {
        LeagueSeason activeSeason = new LeagueSeason(5);

        when(leagueSeasonRepository.findByActiveTrue())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(activeSeason));
        when(leagueSeasonRepository.findTopByOrderBySeasonNumberDesc())
                .thenReturn(Optional.of(new LeagueSeason(4)));
        when(leagueSeasonRepository.saveAndFlush(any(LeagueSeason.class)))
                .thenThrow(new DataIntegrityViolationException("season already created"));

        LeagueSeason result = leagueSeasonService.startNextSeason();

        assertThat(result).isSameAs(activeSeason);
        verify(leagueSeasonRepository).saveAndFlush(any(LeagueSeason.class));
    }
}
