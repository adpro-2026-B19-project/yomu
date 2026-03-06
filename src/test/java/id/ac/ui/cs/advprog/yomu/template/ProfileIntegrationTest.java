package id.ac.ui.cs.advprog.yomu.template;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
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

    @Test
    void profilePageShouldRender() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void profilePageShouldContainBasicSections() throws Exception {
        authRepository.deleteAll();
        authRepository.save(new AuthUser("alice", "alice@example.com", null, "alice", passwordEncoder.encode("CorrectPass1!")));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", "alice@example.com")
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(get("/profile").session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Profile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Name")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Email")));
    }
}
