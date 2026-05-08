package id.ac.ui.cs.advprog.yomu.auth.repository;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<AuthUser, UUID> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findByEmailAndActiveTrue(String email);

    Optional<AuthUser> findByUsernameAndActiveTrue(String username);

    List<AuthUser> findAllByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    @Query("""
            select u from AuthUser u
            where (:keyword is null
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(u.displayName) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%')))
            and (:role is null or u.role = :role)
            and (:active is null or u.active = :active)
            """)
    Page<AuthUser> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") AuthRole role,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
