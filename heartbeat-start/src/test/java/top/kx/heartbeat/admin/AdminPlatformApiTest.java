package top.kx.heartbeat.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.kx.heartbeat.support.MySqlIntegrationTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminPlatformApiTest extends MySqlIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticatesBuildsDynamicMenusAndManagesResources() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokens.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.fields.username").value("admin"));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.dataScope").value("ALL"));

        mockMvc.perform(get("/api/v1/iam/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children").isArray());

        String menuBody = mockMvc.perform(post("/api/v1/iam/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"system\",\"type\":\"MENU\",\"name\":\"测试菜单\","
                                + "\"path\":\"/system/test\",\"component\":\"system/test/index\","
                                + "\"permission\":\"system:test:list\",\"sortNo\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试菜单"))
                .andReturn().getResponse().getContentAsString();

        JsonNode menuJson = objectMapper.readTree(menuBody);
        String menuId = menuJson.at("/data/id").asText();

        mockMvc.perform(put("/api/v1/iam/menus/{id}", menuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"system\",\"type\":\"MENU\",\"name\":\"测试菜单改\","
                                + "\"path\":\"/system/test\",\"component\":\"system/test/index\","
                                + "\"permission\":\"system:test:list\",\"sortNo\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试菜单改"));

        String deptBody = mockMvc.perform(post("/api/v1/admin/resources/depts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试部门\",\"code\":\"test_dept\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试部门"))
                .andReturn().getResponse().getContentAsString();

        JsonNode deptJson = objectMapper.readTree(deptBody);
        String deptId = deptJson.at("/data/id").asText();

        mockMvc.perform(put("/api/v1/admin/resources/depts/{id}", deptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试部门改\",\"code\":\"test_dept\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试部门改"));

        mockMvc.perform(get("/api/v1/admin/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void storesAppearancePreferenceForCurrentUser() throws Exception {
        String userOneId = createUser("appearance_user_" + System.nanoTime());
        String userTwoId = createUser("appearance_user_" + System.nanoTime());

        mockMvc.perform(get("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.colorMode").value("dark"))
                .andExpect(jsonPath("$.data.fluidEnabled").value(true))
                .andExpect(jsonPath("$.data.accentColor").value("#1677ff"))
                .andExpect(jsonPath("$.data.visualStyle").value("glass"));

        mockMvc.perform(put("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorMode\":\"system\",\"fluidEnabled\":true,\"accentColor\":\"#7C5CFC\",\"visualStyle\":\"glass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.colorMode").value("system"))
                .andExpect(jsonPath("$.data.fluidEnabled").value(true))
                .andExpect(jsonPath("$.data.accentColor").value("#7c5cfc"));

        mockMvc.perform(get("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accentColor").value("#7c5cfc"))
                .andExpect(jsonPath("$.data.visualStyle").value("glass"));

        mockMvc.perform(put("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorMode\":\"system\",\"fluidEnabled\":true,\"visualStyle\":\"flat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visualStyle").value("flat"));

        mockMvc.perform(put("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorMode\":\"system\",\"fluidEnabled\":true,\"accentColor\":\"not-a-color\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accentColor").value("#1677ff"));

        mockMvc.perform(get("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.colorMode").value("system"))
                .andExpect(jsonPath("$.data.fluidEnabled").value(true));

        mockMvc.perform(put("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userTwoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorMode\":\"light\",\"fluidEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.colorMode").value("light"))
                .andExpect(jsonPath("$.data.fluidEnabled").value(false));

        mockMvc.perform(get("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userTwoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.colorMode").value("light"))
                .andExpect(jsonPath("$.data.fluidEnabled").value(false));

        mockMvc.perform(get("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.colorMode").value("system"));

        mockMvc.perform(put("/api/v1/auth/preferences/appearance")
                        .header("X-User-Id", userOneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorMode\":\"neon\",\"fluidEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("不支持的颜色模式: neon"));
    }

    @Test
    void restoresTheUserIdentifiedByTheDevelopmentSessionHeader() throws Exception {
        String userBody = mockMvc.perform(post("/api/v1/admin/resources/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"theme_user\",\"nickname\":\"主题用户\","
                                + "\"password\":\"123456\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(userBody).at("/data/id").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.username").value("theme_user"));

        mockMvc.perform(get("/api/v1/iam/routes").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void assignsMenusToRoleAndExposesServerMetrics() throws Exception {
        String roleBody = mockMvc.perform(post("/api/v1/admin/resources/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试角色\",\"code\":\"test_role\",\"dataScope\":\"ALL\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String roleId = objectMapper.readTree(roleBody).at("/data/id").asText();

        mockMvc.perform(get("/api/v1/iam/roles/{id}/menus", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuIds").isArray())
                .andExpect(jsonPath("$.data.menuTree").isArray());

        mockMvc.perform(put("/api/v1/iam/roles/{id}/menus", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuIds\":[\"system-user\",\"system-role\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/iam/roles/{id}/menus", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuIds.length()").value(2));

        mockMvc.perform(get("/api/v1/monitor/server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cpu.cores").isNumber())
                .andExpect(jsonPath("$.data.jvm.heapUsed").isNumber())
                .andExpect(jsonPath("$.data.disk").isArray());

        mockMvc.perform(get("/api/v1/monitor/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").isBoolean())
                .andExpect(jsonPath("$.data.caches").isArray());

        mockMvc.perform(get("/api/v1/monitor/druid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.implementation").isNotEmpty());
    }

    @Test
    void ignoresSpoofedTenantHeadersForTheAuthenticatedSession() throws Exception {
        String created = mockMvc.perform(post("/api/v1/admin/resources/depts")
                        .header("X-Tenant-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"租户 A 部门\",\"code\":\"tenant_a_dept\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value("1"))
                .andReturn().getResponse().getContentAsString();

        String deptId = objectMapper.readTree(created).at("/data/id").asText();

        mockMvc.perform(get("/api/v1/admin/resources/depts")
                        .header("X-Tenant-Id", "888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + deptId + "')]").exists());
    }

    @Test
    void managesSocialProviderConfigurationThroughTheAdminApi() throws Exception {
        String providerCode = "OIDC_" + System.nanoTime();
        String created = mockMvc.perform(post("/api/v1/admin/resources/social-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"" + providerCode + "\",\"name\":\"Enterprise SSO\","
                                + "\"appId\":\"client-id\",\"appSecret\":\"client-secret\","
                                + "\"redirectUri\":\"https://example.test/callback\","
                                + "\"authorizeUrl\":\"https://idp.example.test/authorize?client_id={appId}"
                                + "&redirect_uri={redirectUri}&state={state}\","
                                + "\"autoRegister\":true,\"status\":\"ACTIVE\",\"sortNo\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value(providerCode))
                .andExpect(jsonPath("$.data.appId").value("client-id"))
                .andReturn().getResponse().getContentAsString();

        String providerId = objectMapper.readTree(created).at("/data/id").asText();

        mockMvc.perform(get("/api/v1/admin/resources/social-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + providerId + "')]").exists());

        mockMvc.perform(put("/api/v1/admin/resources/social-providers/{id}", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Enterprise Login\",\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Enterprise Login"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(delete("/api/v1/admin/resources/social-providers/{id}", providerId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/resources/social-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + providerId + "')]").doesNotExist());
    }

    @Test
    void completesMockSocialLoginThroughTheHandlerRegistry() throws Exception {
        String authorizeBody = mockMvc.perform(get("/api/v1/auth/social/MOCK/authorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizeUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String state = objectMapper.readTree(authorizeBody).at("/data/state").asText();

        mockMvc.perform(get("/api/v1/auth/social/MOCK/callback")
                        .param("code", "mock:registry-user-" + System.nanoTime())
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.id").isNotEmpty());
    }

    private String createUser(String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admin/resources/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"nickname\":\"Appearance User\","
                                + "\"password\":\"123456\",\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/id").asText();
    }
}
