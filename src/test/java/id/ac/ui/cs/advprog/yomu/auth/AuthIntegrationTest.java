package id.ac.ui.cs.advprog.yomu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthRepository authRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanDatabase() {
        authRepository.deleteAll();
    }

    @Test
    void registerPageShouldRender() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("form"));
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
                .andExpect(model().attributeExists("registeredEmail"))
                .andExpect(model().attributeExists("registeredHashedPassword"));
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
                .andExpect(flash().attributeExists("registeredEmail"))
                .andExpect(flash().attributeExists("registeredHashedPassword"));

        assertThat(authRepository.count()).isEqualTo(before + 1);
        AuthUser user = authRepository.findByUsername("demo-user").orElseThrow();
        assertThat(user.getEmail()).isEqualTo("demo@example.com");
        assertThat(passwordEncoder.matches("SafePassword1!", user.getPassword())).isTrue();
    }

    @Test
    void registerShouldUseEmailLocalPartWhenUsernameBlank() throws Exception {
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("email", "nora@example.com")
                        .param("username", "   ")
                        .param("password", "NoraPassword1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attribute("registeredName", "nora"));

        assertThat(authRepository.count()).isEqualTo(before + 1);
        assertThat(authRepository.findByUsername("nora")).isPresent();
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
                .andExpect(redirectedUrl("/auth/register"));

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
                .andExpect(redirectedUrl("/auth/register"));

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
                .andExpect(flash().attribute("warning", "Email does not exist"));

        assertThat(authRepository.count()).isEqualTo(before);
    }

    @Test
    void loginShouldFailWhenEmailNotRegistered() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("email", "ghost@example.com")
                        .param("password", "GhostPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attribute("loginWarning", "Invalid email or password"));
    }

    @Test
    void loginShouldFailWhenPasswordInvalid() throws Exception {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("email", "alice@example.com")
                        .param("password", "WrongPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attribute("loginWarning", "Invalid email or password"));
    }

    @Test
    void loginShouldSucceedWhenCredentialsAreValid() throws Exception {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("email", "alice@example.com")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attribute("loggedInName", "alice"))
                .andExpect(flash().attribute("loggedInEmail", "alice@example.com"));
    }
}
