package id.ac.ui.cs.advprog.yomu.league.controller;

import id.ac.ui.cs.advprog.yomu.league.dto.QuizCompletionApiEventRequest;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.service.ClanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/league")
public class LeagueIntegrationRestController {

    private final ClanService clanService;

    public LeagueIntegrationRestController(ClanService clanService) {
        this.clanService = clanService;
    }

    @PostMapping("/events/quiz-completions")
    public ResponseEntity<Void> ingestQuizCompletion(@Valid @RequestBody QuizCompletionApiEventRequest request) {
        try {
            clanService.recordQuizCompletion(new ClanService.QuizCompletionPayload(
                    request.eventId(),
                    request.userId(),
                    request.textId(),
                    request.score(),
                    request.accuracy(),
                    request.completedAt()
            ));
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/leaderboard/bronze")
    public ClanService.LeaderboardPage bronzeLeaderboard(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size
    ) {
        return clanService.getLeaderboardPage(TierCode.BRONZE, page, size);
    }

    @GetMapping("/leaderboard/{tierCode}")
    public ClanService.LeaderboardPage leaderboardByTier(
            @org.springframework.web.bind.annotation.PathVariable String tierCode,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size
    ) {
        try {
            return clanService.getLeaderboardPage(TierCode.valueOf(tierCode.trim().toUpperCase()), page, size);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tier code", exception);
        }
    }
}
