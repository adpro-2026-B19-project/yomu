package id.ac.ui.cs.advprog.yomu.reading.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "text_id", nullable = false)
    private Text text;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Double accuracy;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public QuizAttempt() {}

    public QuizAttempt(Text text, String userId, Double score, Double accuracy) {
        this.text = text;
        this.userId = userId;
        this.score = score;
        this.accuracy = accuracy;
        this.timestamp = Instant.now();
    }
}
