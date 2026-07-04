package top.kx.heartbeat.security;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityAuthorizationApiTest {

//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private SysUserRolePlatformMapper sysUserRoleMapper;
//
//    @Autowired
//    private SysPermissionMapper sysPermissionMapper;
//
//    @Autowired
//    private SysRolePermissionMapper sysRolePermissionMapper;
//
//    @Test
//    void requiresJwtAndEnforcesButtonPermissions() throws Exception {
//        mockMvc.perform(get("/api/v1/admin/resources/users"))
//                .andExpect(status().isUnauthorized());
//
//        String adminToken = login("admin", "admin123");
//        String suffix = String.valueOf(System.nanoTime());
//        String username = "limited_" + suffix;
//
//        String userBody = mockMvc.perform(post("/api/v1/admin/resources/users")
//                        .header("Authorization", "Bearer " + adminToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"username\":\"" + username + "\",\"password\":\"limited123\","
//                                + "\"nickname\":\"Limited User\",\"status\":\"ENABLED\"}"))
//                .andExpect(status().isOk())
//                .andReturn().getResponse().getContentAsString();
//        long userId = objectMapper.readTree(userBody).at("/data/id").asLong();
//
//        String roleBody = mockMvc.perform(post("/api/v1/admin/resources/roles")
//                        .header("Authorization", "Bearer " + adminToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"name\":\"Read Only User\",\"code\":\"limited_" + suffix + "\","
//                                + "\"dataScope\":\"SELF\",\"status\":\"ENABLED\"}"))
//                .andExpect(status().isOk())
//                .andReturn().getResponse().getContentAsString();
//        long roleId = objectMapper.readTree(roleBody).at("/data/id").asLong();
//
//        SysUserRoleEntity userRole = new SysUserRoleEntity();
//        userRole.setTenantId(1L);
//        userRole.setUserId(userId);
//        userRole.setRoleId(roleId);
//        userRole.setCreatedBy(1L);
//        userRole.setCreateTime(LocalDateTime.now());
//        TenantContext.runAsPlatform(() -> {
//            sysUserRoleMapper.insert(userRole);
//            SysPermissionEntity listPermission = sysPermissionMapper.selectOneByQuery(QueryWrapper.create()
//                    .where("tenant_id", 1L)
//                    .and("permission_code", "system:user:list")
//                    .and("delete_marker = 0"));
//            SysRolePermissionEntity rolePermission = new SysRolePermissionEntity();
//            rolePermission.setTenantId(1L);
//            rolePermission.setRoleId(roleId);
//            rolePermission.setPermissionId(listPermission.getId());
//            rolePermission.setCreatedBy(1L);
//            rolePermission.setCreateTime(LocalDateTime.now());
//            sysRolePermissionMapper.insert(rolePermission);
//            return null;
//        });
//
//        String limitedToken = login(username, "limited123");
//
//        mockMvc.perform(get("/api/v1/admin/resources/users")
//                        .header("Authorization", "Bearer " + limitedToken))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.length()").value(1))
//                .andExpect(jsonPath("$.data[0].id").value(String.valueOf(userId)));
//
//        mockMvc.perform(post("/api/v1/admin/resources/users")
//                        .header("Authorization", "Bearer " + limitedToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"username\":\"forbidden\",\"password\":\"123456\"}"))
//                .andExpect(status().isForbidden());
//
//        mockMvc.perform(get("/api/v1/monitor/server")
//                        .header("Authorization", "Bearer " + limitedToken))
//                .andExpect(status().isForbidden());
//
//        mockMvc.perform(get("/api/v1/tool/gen/tables")
//                        .header("Authorization", "Bearer " + limitedToken))
//                .andExpect(status().isForbidden());
//    }
//
//    private String login(String username, String password) throws Exception {
//        String body = mockMvc.perform(post("/api/v1/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
//                .andExpect(status().isOk())
//                .andReturn().getResponse().getContentAsString();
//        return objectMapper.readTree(body).at("/data/accessToken").asText();
//    }
}
