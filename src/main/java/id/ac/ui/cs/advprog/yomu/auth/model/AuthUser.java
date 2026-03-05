package id.ac.ui.cs.advprog.yomu.auth.model;

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
@Table(name = "auth_users")
@SuppressWarnings("JpaDataSourceORMInspection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column
    private Long phoneNumber;

    @Column(nullable = false)
    private String displayName;

    @Column
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuthUser(String displayName) {
        this.displayName = displayName;
    }

    public AuthUser(String email, Long phoneNumber, String displayName, String password) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.password = password;
    }

    public String getUsername() {
        return displayName;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
