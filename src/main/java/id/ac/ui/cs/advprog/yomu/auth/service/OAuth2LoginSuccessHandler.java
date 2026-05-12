package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendRedirect("/auth/login?error");
            return;
        }

        authRepository.findByEmail(email).orElseGet(() -> {
            String username = generateUsernameFromEmail(email);

            AuthUser newUser = new AuthUser(
                    username,
                    email,
                    null,
                    name != null && !name.isBlank() ? name : username,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    AuthRole.USER
            );

            return authRepository.save(newUser);
        });

        response.sendRedirect("/profile");
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();

        if (base.isBlank()) {
            base = "googleuser";
        }

        String username = base;
        int counter = 1;

        while (authRepository.findByUsername(username).isPresent()) {
            username = base + counter;
            counter++;
        }

        return username;
    }
}