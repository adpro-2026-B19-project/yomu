package id.ac.ui.cs.advprog.yomu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
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

    @BeforeEach
    void cleanDatabase() {
        authRepository.deleteAll();
    }

    @Test
    void authPageShouldRender() throws Exception {
        mockMvc.perform(get("/auth"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/index"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void registerShouldPersistUserIntoDatabase() throws Exception {
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "demo-user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth"));

        assertThat(authRepository.count()).isEqualTo(before + 1);
        assertThat(authRepository.findByUsername("demo-user")).isPresent();
    }

    @Test
    void registerShouldRejectBlankUsername() throws Exception {
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth"));

        assertThat(authRepository.count()).isEqualTo(before);
    }

    @Test
    void registerShouldRejectDuplicateUsername() throws Exception {
        authRepository.save(new AuthUser("existing-user"));
        long before = authRepository.count();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("username", "existing-user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth"));

        assertThat(authRepository.count()).isEqualTo(before);
    }
}
