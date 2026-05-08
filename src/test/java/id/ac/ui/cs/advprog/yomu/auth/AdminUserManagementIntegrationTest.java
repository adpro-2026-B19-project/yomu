package id.ac.ui.cs.advprog.yomu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserManagementIntegrationTest {

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
    void unauthenticatedUserCannotAccessAdminUsersPage() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void pelajarCannotAccessAdminUsersPage() throws Exception {
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "Alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        MvcResult loginResult = loginAs("alice@example.com", "CorrectPass1!");

        mockMvc.perform(get("/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminUsersPage() throws Exception {
        authRepository.save(new AuthUser("admin", "admin@example.com", null, "Admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));
        AuthUser user = authRepository.save(new AuthUser("alice", "alice@example.com", null, "Alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");

        mockMvc.perform(get("/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Manage user accounts")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/users/" + user.getId())));
    }

    @Test
    void searchReturnsMatchingUsers() throws Exception {
        authRepository.save(new AuthUser("admin", "admin@example.com", null, "Admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "Alice Reader", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        authRepository.save(new AuthUser("bob", "bob@example.com", null, "Bob Reader", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));
        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");

        mockMvc.perform(get("/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .param("keyword", "alice"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alice@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bob@example.com"))));
    }

    @Test
    void roleAndStatusFiltersReturnCorrectUsers() throws Exception {
        authRepository.save(new AuthUser("admin", "admin@example.com", null, "Admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));

        AuthUser activeUser = new AuthUser("active-user", "enabled@example.com", null, "Active User", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER);
        authRepository.save(activeUser);
        AuthUser inactiveUser = new AuthUser("inactive-user", "inactive@example.com", null, "Inactive User", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER);
        inactiveUser.deactivate();
        authRepository.save(inactiveUser);

        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");

        mockMvc.perform(get("/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .param("role", "USER")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inactive@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("enabled@example.com"))));
    }

    @Test
    void sensitiveFieldsAreNotRendered() throws Exception {
        String hash = passwordEncoder.encode("SensitivePass1!");
        authRepository.save(new AuthUser("admin", "admin@example.com", null, "Admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "Alice", hash, AuthRole.USER));
        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");

        mockMvc.perform(get("/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(hash))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
    }

    @Test
    void adminCanUpdateUserStatus() throws Exception {
        authRepository.save(new AuthUser("admin", "admin@example.com", null, "Admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));
        AuthUser alice = authRepository.save(new AuthUser("alice", "alice@example.com", null, "Alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));

        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");
        mockMvc.perform(post("/admin/users/{id}/status", alice.getId())
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("active", "false")
                        .param("confirmation", "DEACTIVATE"))
                .andExpect(status().is3xxRedirection());

        AuthUser updated = authRepository.findById(alice.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getDeletedAt()).isNotNull();
    }

    @Test
    void adminDeactivationRequiresConfirmationText() throws Exception {
        authRepository.save(new AuthUser("admin", "admin@example.com", null, "Admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));
        AuthUser alice = authRepository.save(new AuthUser("alice", "alice@example.com", null, "Alice", passwordEncoder.encode("CorrectPass1!"), AuthRole.USER));

        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");
        mockMvc.perform(post("/admin/users/{id}/status", alice.getId())
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("active", "false")
                        .param("confirmation", "WRONG"))
                .andExpect(status().is3xxRedirection());

        AuthUser updated = authRepository.findById(alice.getId()).orElseThrow();
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void adminRoleDoesNotRenderDeactivateAction() throws Exception {
        AuthUser admin = authRepository.save(new AuthUser("admin", "admin@example.com", null, "Admin", passwordEncoder.encode("AdminPass1!"), AuthRole.ADMIN));
        MvcResult loginResult = loginAs("admin@example.com", "AdminPass1!");

        mockMvc.perform(get("/admin/users")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Protected")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/admin/users/" + admin.getId() + "/status"))));
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
