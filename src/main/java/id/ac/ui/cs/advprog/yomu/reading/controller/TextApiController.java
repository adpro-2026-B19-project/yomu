package id.ac.ui.cs.advprog.yomu.reading.controller;

import id.ac.ui.cs.advprog.yomu.reading.dto.UserReadingStatResponse;
import id.ac.ui.cs.advprog.yomu.reading.service.IQuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reading/stats")
public class TextApiController {

    private final IQuizService quizService;

    public TextApiController(IQuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserReadingStatResponse> getUserStats(@PathVariable String userId) {
        UserReadingStatResponse stats = quizService.getUserReadingStats(userId);
        return ResponseEntity.ok(stats);
    }
}
