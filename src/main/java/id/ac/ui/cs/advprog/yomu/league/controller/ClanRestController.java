package id.ac.ui.cs.advprog.yomu.league.controller;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.service.CurrentUserResolver;
import id.ac.ui.cs.advprog.yomu.league.dto.CreateClanApiRequest;
import id.ac.ui.cs.advprog.yomu.league.service.ClanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clans")
public class ClanRestController {

    private final ClanService clanService;
    private final CurrentUserResolver currentUserResolver;

    public ClanRestController(ClanService clanService, CurrentUserResolver currentUserResolver) {
        this.clanService = clanService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public List<ClanService.ClanSummary> listClans() {
        return clanService.listClans();
    }

    @PostMapping
    public ResponseEntity<ClanService.ClanSummary> createClan(
            @Valid @RequestBody CreateClanApiRequest request,
            Authentication authentication
    ) {
        AuthUser currentUser = currentUserResolver.resolveUser(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated"));
        try {
            ClanService.ClanSummary createdClan = clanService.createClan(
                    new ClanService.CreateClanRequest(request.name()),
                    currentUser.getId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClan);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
