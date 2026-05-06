package id.ac.ui.cs.advprog.yomu.league.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "league_seasons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("JpaDataSourceORMInspection")
public class LeagueSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private int seasonNumber;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime endedAt;

    public LeagueSeason(int seasonNumber) {
        this.seasonNumber = seasonNumber;
        this.active = true;
    }

    public void end() {
        this.active = false;
        this.endedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
