package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse;
import id.ac.ui.cs.advprog.yomu.reading.service.TextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reading/stats")
public class TextApiController {

    private final TextService textService;

    public TextApiController(TextService textService) {
        this.textService = textService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserReadingStatResponse> getUserStats(@PathVariable String userId) {
        UserReadingStatResponse stats = textService.getUserReadingStats(userId);
        return ResponseEntity.ok(stats);
    }
}
