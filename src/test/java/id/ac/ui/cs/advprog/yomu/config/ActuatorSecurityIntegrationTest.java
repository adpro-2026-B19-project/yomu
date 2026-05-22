package id.ac.ui.cs.advprog.yomu.config;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.auth.service.InMemoryRequestRateLimiter;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

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
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void actuatorEndpointsRejectNonAdminUsers() throws Exception {
        authRepository.save(new AuthUser(
                "reader",
                "reader@example.com",
                null,
                "Reader",
                passwordEncoder.encode("ReaderPass1!"),
                AuthRole.USER
        ));
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "reader@example.com")
                        .param("password", "ReaderPass1!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        HttpSession session = loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/actuator/metrics")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadHealthAndMetrics() throws Exception {
        authRepository.save(new AuthUser(
                "admin",
                "admin@example.com",
                null,
                "Admin",
                passwordEncoder.encode("AdminPass1!"),
                AuthRole.ADMIN
        ));
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("identifier", "admin@example.com")
                        .param("password", "AdminPass1!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        HttpSession session = loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/actuator/health")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));

        mockMvc.perform(get("/actuator/metrics")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());

        mockMvc.perform(get("/actuator/prometheus")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }
}
