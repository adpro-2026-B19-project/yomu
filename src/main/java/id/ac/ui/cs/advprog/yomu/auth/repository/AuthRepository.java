package id.ac.ui.cs.advprog.yomu.auth.repository;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthRepository extends JpaRepository<AuthUser, UUID> {
    List<AuthUser> findAllByOrderByIdDesc();

    @Query("select u from AuthUser u where u.displayName = :username")
    Optional<AuthUser> findByUsername(@Param("username") String username);
}
