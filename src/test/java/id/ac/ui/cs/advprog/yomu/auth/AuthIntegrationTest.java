package id.ac.ui.cs.advprog.yomu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.auth.service.InMemoryRequestRateLimiter;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private InMemoryRequestRateLimiter requestRateLimiter;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanDatabase() {
        authRepository.deleteAll();
        requestRateLimiter.clearAll();
    }

    @Test
    void registerPageShouldRender() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attributeExists("suggestedUsername"));
    }

    @Test
    void rootShouldRenderLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landingPage"));
    }

    @Test
    void loginPageShouldRender() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginForm"))
                .andExpect(model().attributeExists("registeredName"))
                .andExpect(model().attributeExists("registeredEmail"));
    }

    @Test
    void registerShouldPersistUserIntoDatabase() throws Exception {
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("email", "demo@example.com")
                        .param("username", "demo-user")
                        .param("password", "SafePassword1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("registeredName"))
                .andExpect(flash().attributeExists("registeredEmail"));

        assertThat(authRepository.count()).isEqualTo(before + 1);
        AuthUser user = authRepository.findByUsername("demo-user").orElseThrow();
        assertThat(user.getEmail()).isEqualTo("demo@example.com");
        assertThat(passwordEncoder.matches("SafePassword1!", user.getPassword())).isTrue();
    }

    @Test
    void registerShouldRejectBlankUsername() throws Exception {
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("email", "nora@example.com")
                        .param("username", "   ")
                        .param("password", "NoraPassword1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attribute("warning", "Username is required"));

        assertThat(authRepository.count()).isEqualTo(before);
        assertThat(authRepository.findByUsername("nora")).isNotPresent();
    }

    @Test
    void registerShouldRejectDuplicateEmail() throws Exception {
        authRepository.save(new AuthUser("existing-user", "existing@example.com", null, "existing-user", "hashed"));
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("email", "existing@example.com")
                        .param("username", "new-user")
                        .param("password", "ExistingPassword1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attribute("warning", "Registration could not be completed with the provided details"));

        assertThat(authRepository.count()).isEqualTo(before);
    }

    @Test
    void registerShouldRejectDuplicateUsername() throws Exception {
        authRepository.save(new AuthUser("existing-user", "existing@example.com", null, "existing-user", "hashed"));
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("email", "new@example.com")
                        .param("username", "existing-user")
                        .param("password", "NewPassword1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attribute("warning", "Username is already taken"));

        assertThat(authRepository.count()).isEqualTo(before);
    }

    @Test
    void registerShouldRejectWeakPasswordAndShowWarning() throws Exception {
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("email", "weak@example.com")
                        .param("username", "weak-user")
                        .param("password", "weakpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attribute("warning", "Password is too weak"));

        assertThat(authRepository.count()).isEqualTo(before);
    }

    @Test
    void registerShouldRejectNonexistentEmailAndShowWarning() throws Exception {
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("email", "ghost@missing.invalid")
                        .param("username", "ghost-user")
                        .param("password", "GhostPassword1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attribute("warning", "Registration could not be completed with the provided details"));

        assertThat(authRepository.count()).isEqualTo(before);
    }

    @Test
    void loginShouldFailWhenEmailNotRegistered() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "ghost@example.com")
                        .param("password", "GhostPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void loginShouldFailWhenPasswordInvalid() throws Exception {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "alice@example.com")
                        .param("password", "WrongPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void loginShouldSucceedWhenCredentialsAreValid() throws Exception {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "alice@example.com")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", org.hamcrest.Matchers.notNullValue()))
                .andReturn();

        HttpSession session = loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/profile").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user",
                        org.hamcrest.Matchers.hasProperty("username", org.hamcrest.Matchers.is("alice"))))
                .andExpect(model().attribute("user",
                        org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("alice@example.com"))));
    }

    @Test
    void loginShouldSucceedWithIdentifierFieldWhenIdentifierIsEmail() throws Exception {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "alice@example.com")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginShouldSucceedWithIdentifierFieldWhenIdentifierIsUsername() throws Exception {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        authRepository.save(new AuthUser("alice-user", "alice@example.com", null, "alice", hashedPassword));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "alice-user")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginPageShouldIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Referrer-Policy", "no-referrer"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")));
    }

    @Test
    void loginShouldRejectPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .param("identifier", "alice@example.com")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerShouldRejectPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("email", "demo@example.com")
                        .param("username", "demo-user")
                        .param("password", "SafePassword1!"))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedFailedLoginAttemptsAreRateLimited() throws Exception {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login").with(csrf())
                            .param("identifier", "alice@example.com")
                            .param("password", "WrongPass" + i + "!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login?error"));
        }

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("identifier", "alice@example.com")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void repeatedRegisterAttemptsAreRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/register").with(csrf())
                            .param("email", "register" + i + "@missing.invalid")
                            .param("username", "reg-user-" + i)
                            .param("password", "SafePassword1!"))
                    .andExpect(status().is3xxRedirection());
        }

        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email", "register-blocked@missing.invalid")
                        .param("username", "reg-user-blocked")
                        .param("password", "SafePassword1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attribute("warning", "Unable to process registration right now. Please try again later."));
    }

    @Test
    void loginInjectionPayloadDoesNotBypassAuthentication() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "' OR '1'='1")
                        .param("password", "' OR '1'='1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }
}
