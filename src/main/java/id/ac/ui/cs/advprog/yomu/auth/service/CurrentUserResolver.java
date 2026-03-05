package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.util.Optional;
import org.springframework.security.core.Authentication;

public interface CurrentUserResolver {
    Optional<AuthUser> resolveUser(Authentication authentication);

    Optional<String> resolveUsername(Authentication authentication);
}
