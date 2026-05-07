package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final AuthRepository authRepository;
    private final AuthIdentifierValidator authIdentifierValidator;

    public AuthUserDetailsService(
            AuthRepository authRepository,
            AuthIdentifierValidator authIdentifierValidator
    ) {
        this.authRepository = authRepository;
        this.authIdentifierValidator = authIdentifierValidator;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String normalizedIdentifier = authIdentifierValidator.normalize(identifier);
        AuthUser user = loadUserByIdentifier(normalizedIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        String password = user.getPassword();
        if (password == null || password.isBlank()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        return new AuthenticatedUserPrincipal(
                user.getUsername(),
                user.getEmail(),
                password,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    private java.util.Optional<AuthUser> loadUserByIdentifier(String normalizedIdentifier) {
        AuthIdentifierValidator.IdentifierType identifierType = authIdentifierValidator.classify(normalizedIdentifier);
        if (identifierType == AuthIdentifierValidator.IdentifierType.EMAIL) {
            return authRepository.findByEmail(normalizedIdentifier);
        }
        if (identifierType == AuthIdentifierValidator.IdentifierType.USERNAME) {
            return authRepository.findByUsername(normalizedIdentifier);
        }
        return java.util.Optional.empty();
    }
}
