package id.ac.ui.cs.advprog.yomu.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileIntegrationTest {

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
    void profilePageShouldRender() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void profilePageShouldContainBasicSections() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!")));

        MvcResult loginResult = loginAs("alice@example.com", "CorrectPass1!");

        mockMvc.perform(get("/profile").session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Profile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Edit Profile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Save Profile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Revert Changes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/profile\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"username\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"phoneAreaCode\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"phoneLocalNumber\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"email\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[data-auto-dismiss='true']")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Email")));
    }

    @Test
    void profileUpdateShouldPersistChangesAndShowSuccessFeedback() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!")));
        MvcResult loginResult = loginAs("alice@example.com", "CorrectPass1!");

        mockMvc.perform(post("/profile")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", "alice-updated")
                        .param("displayName", "Alice Updated")
                        .param("phoneNumber", "628123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("success", "Profile updated successfully"));

        AuthUser updatedUser = authRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(updatedUser.getUsername()).isEqualTo("alice-updated");
        assertThat(updatedUser.getDisplayName()).isEqualTo("Alice Updated");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(628123456789L);
    }

    @Test
    void profileUpdateShouldRejectDuplicateUsernameAndReturnWarning() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!")));
        authRepository.save(new AuthUser("taken-user", "taken@example.com", null, "taken", passwordEncoder.encode("CorrectPass1!")));
        MvcResult loginResult = loginAs("alice@example.com", "CorrectPass1!");

        mockMvc.perform(post("/profile")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", "taken-user")
                        .param("displayName", "Alice Updated")
                        .param("phoneNumber", "628123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("warning", "Username is already taken"));

        AuthUser originalUser = authRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(originalUser.getUsername()).isEqualTo("alice");
    }

    @Test
    void pelajarCanAccessDeleteConfirmationPage() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        MvcResult loginResult = loginAs("alice@example.com", "CorrectPass1!");

        mockMvc.perform(get("/profile/delete").session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/delete"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Enter your password")));
    }

    @Test
    void nonPelajarCannotAccessDeleteAction() throws Exception {
        authRepository.save(new AuthUser("admin", "admin@example.com", null, "admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));
        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");

        mockMvc.perform(get("/profile/delete")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/profile/delete")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("password", "AdminPass1!"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordDoesNotDeleteAccount() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        MvcResult loginResult = loginAs("alice@example.com", "CorrectPass1!");

        mockMvc.perform(post("/profile/delete")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("password", "WrongPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/delete"))
                .andExpect(flash().attribute("warning", "Unable to delete account. Please check your password and try again."));

        AuthUser user = authRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
    }

    @Test
    void correctPasswordDeletesAccountAndInvalidatesSession() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession) loginAs("alice@example.com", "CorrectPass1!").getRequest().getSession(false);

        mockMvc.perform(post("/profile/delete")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?deleted"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Set-Cookie", org.hamcrest.Matchers.containsString("JSESSIONID")));

        AuthUser user = authRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(user.isActive()).isFalse();
        assertThat(user.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void deletedAccountCannotLogIn() throws Exception {
        AuthUser user = new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER);
        user.deactivate();
        authRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("identifier", "alice@example.com")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void postingProfileUpdateCannotModifyAnotherUserAccount() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        authRepository.save(new AuthUser("bob", "bob@example.com", null, "bob", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        MvcResult aliceLogin = loginAs("alice@example.com", "CorrectPass1!");

        mockMvc.perform(post("/profile")
                        .session((org.springframework.mock.web.MockHttpSession) aliceLogin.getRequest().getSession(false))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", "alice-updated")
                        .param("displayName", "Alice Updated")
                        .param("phoneNumber", "628123456789"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        AuthUser bob = authRepository.findByEmail("bob@example.com").orElseThrow();
        assertThat(bob.getUsername()).isEqualTo("bob");
        assertThat(bob.getDisplayName()).isEqualTo("bob");
        assertThat(bob.getPhoneNumber()).isNull();
    }

    private MvcResult loginAs(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("identifier", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }
}
