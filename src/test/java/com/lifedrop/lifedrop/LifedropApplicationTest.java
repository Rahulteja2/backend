package com.lifedrop.lifedrop;

import com.lifedrop.controller.AdminController;
import com.lifedrop.controller.HealthController;
import com.lifedrop.model.BloodGroup;
import com.lifedrop.model.Donor;
import com.lifedrop.model.Role;
import com.lifedrop.model.User;
import com.lifedrop.repo.DonorRepository;
import com.lifedrop.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LifedropApplicationTest {
    @Autowired
    private HealthController health;

    @Autowired
    private AdminController admin;

    @Autowired
    private UserRepository users;

    @Autowired
    private DonorRepository donors;

    @Autowired
    @Qualifier("corsConfigurationSource")
    private CorsConfigurationSource cors;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {}

    @Test
    void healthReportsDatabaseAvailabilityWithoutSensitiveDetails() {
        assertThat(health.health()).isEqualTo(java.util.Map.of("status", "UP"));
    }

    @Test
    void healthHttpEndpointReturnsDatabaseStatus() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void adminHttpEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsConfiguredOriginsWithoutCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        CorsConfiguration configuration = cors.getCorsConfiguration(request);
        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(configuration.getAllowCredentials()).isFalse();
    }

    @Test
    void adminApiDoesNotExposePasswordHashesOrAllowDeletingAdmins() {
        var seededAdmin = users.findAll().stream()
            .filter(user -> user.getRole() == Role.ADMIN)
            .findFirst()
            .orElseThrow();
        assertThat(objectMapper.writeValueAsString(seededAdmin).contains("passwordHash")).isFalse();
        assertThat(objectMapper.writeValueAsString(admin.users())).doesNotContain("passwordHash");
        assertThatThrownBy(() -> admin.deleteUser(seededAdmin.getId()))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void deletingUserAlsoDeletesTheirDonorProfile() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("remove-user@example.test");
        user.setPasswordHash("test-hash");
        user.setRole(Role.USER);
        user = users.save(user);

        Donor donor = new Donor();
        donor.setUserEmail(user.getEmail());
        donor.setName(user.getName());
        donor.setBloodGroup(BloodGroup.O_POS);
        donor.setCity("Test City");
        donors.save(donor);

        admin.deleteUser(user.getId());

        assertThat(users.findById(user.getId())).isEmpty();
        assertThat(donors.findByUserEmail(user.getEmail())).isEmpty();
    }
}
