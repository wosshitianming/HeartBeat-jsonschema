package top.kx.heartbeat.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.kx.heartbeat.domain.platform.PlatformAdministrationRepository;
import top.kx.heartbeat.infrastructure.security.JwtTokenService;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TypedAuthRepositoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PlatformAdministrationRepository repository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void loginIssuesTenantScopedSessionTokenAndLoadsPermissionsFromPermissionRelations() throws Exception {
        JsonNode login = login("admin", "admin123");
        String accessToken = login.at("/data/accessToken").asText();

        assertTrue(accessToken.length() > 20);
        assertTrue(jwtTokenService.parseTenantId(accessToken) > 0);
        assertTrue(jwtTokenService.parseSessionId(accessToken).length() > 20);

        TenantContext.setTenantId(jwtTokenService.parseTenantId(accessToken));
        List<String> permissions = repository.listPermissionsByUserId(jwtTokenService.parseUserId(accessToken));

        assertTrue(permissions.contains("system:user:list"));
        assertTrue(permissions.contains("system:role:grant"));
        assertFalse(permissions.contains(""));
    }

    @Test
    void authenticatedRequestRestoresTenantFromJwtAndIgnoresSpoofedTenantHeader() throws Exception {
        JsonNode login = login("admin", "admin123");
        String accessToken = login.at("/data/accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-Id", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.tenantId").value("1"))
                .andExpect(jsonPath("$.data.permissions[?(@ == 'system:user:list')]").exists());
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
