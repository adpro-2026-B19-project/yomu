package id.ac.ui.cs.advprog.yomu.league.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "league_clan_join_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("JpaDataSourceORMInspection")
public class ClanJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_id", nullable = false)
    private Clan clan;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClanJoinRequestStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private UUID reviewedByUserId;

    @Column
    private LocalDateTime reviewedAt;

    public ClanJoinRequest(Clan clan, UUID requesterUserId) {
        this.clan = clan;
        this.requesterUserId = requesterUserId;
        this.status = ClanJoinRequestStatus.PENDING;
    }

    public void approve(UUID reviewerUserId) {
        ensurePending();
        this.status = ClanJoinRequestStatus.APPROVED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(UUID reviewerUserId) {
        ensurePending();
        this.status = ClanJoinRequestStatus.REJECTED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = LocalDateTime.now();
    }

    private void ensurePending() {
        if (status != ClanJoinRequestStatus.PENDING) {
            throw new IllegalStateException("Join request has already been processed");
        }
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ClanJoinRequestStatus.PENDING;
        }
    }
}
