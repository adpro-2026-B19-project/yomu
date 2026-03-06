package id.ac.ui.cs.advprog.yomu.auth.repository;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<AuthUser, UUID> {
    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findByUsername(String username);
}
