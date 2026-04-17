package id.ac.ui.cs.advprog.yomu.league.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "league_clan_quiz_score_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_league_score_event_id", columnNames = "event_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("JpaDataSourceORMInspection")
public class ClanQuizScoreEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "clan_id", nullable = false, updatable = false)
    private UUID clanId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "text_id", nullable = false, updatable = false)
    private UUID textId;

    @Column(nullable = false, updatable = false)
    private double score;

    @Column(nullable = false, updatable = false)
    private double accuracy;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private LocalDateTime completedAt;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public ClanQuizScoreEvent(
            UUID eventId,
            UUID clanId,
            UUID userId,
            UUID textId,
            double score,
            double accuracy,
            LocalDateTime completedAt
    ) {
        this.eventId = eventId;
        this.clanId = clanId;
        this.userId = userId;
        this.textId = textId;
        this.score = score;
        this.accuracy = accuracy;
        this.completedAt = completedAt;
    }

    @PrePersist
    void prePersist() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}
