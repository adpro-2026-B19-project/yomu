package id.ac.ui.cs.advprog.yomu.auth.repository;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<AuthUser, Long> {
    List<AuthUser> findAllByOrderByIdDesc();

    Optional<AuthUser> findByUsername(String username);
}
