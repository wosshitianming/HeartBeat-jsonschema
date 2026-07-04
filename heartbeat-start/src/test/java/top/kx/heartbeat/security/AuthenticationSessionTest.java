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
class AuthenticationSessionTest {

//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private JwtTokenService jwtTokenService;
//
//    @Autowired
//    private JwtProperties jwtProperties;
//
//    @Autowired
//    private AuthenticationSessionService authenticationSessionService;
//
//    @Autowired
//    private AuthSessionRepository authSessionRepository;
//
//    @Autowired
//    private AuthSessionMapper authSessionMapper;
//
//    @Test
//    void activeSessionCanAccessProtectedApi() throws Exception {
//        JsonNode login = login();
//        String accessToken = login.at("/data/accessToken").asText();
//
//        mockMvc.perform(get("/api/v1/auth/me")
//                        .header("Authorization", "Bearer " + accessToken))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.id").value("1"));
//    }
//
//    @Test
//    void revokedSessionCannotAccessProtectedApi() throws Exception {
//        JsonNode login = login();
//        String accessToken = login.at("/data/accessToken").asText();
//        long tenantId = jwtTokenService.parseTenantId(accessToken);
//        String sessionId = jwtTokenService.parseSessionId(accessToken);
//
//        authSessionRepository.revoke(tenantId, sessionId, LocalDateTime.now());
//
//        mockMvc.perform(get("/api/v1/auth/me")
//                        .header("Authorization", "Bearer " + accessToken))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void expiredAccessTokenCannotAccessProtectedApi() throws Exception {
//        JsonNode login = login();
//        String accessToken = login.at("/data/accessToken").asText();
//        String expiredAccessToken = token(
//                jwtTokenService.parseUserId(accessToken),
//                "admin",
//                jwtTokenService.parseTenantId(accessToken),
//                jwtTokenService.parseSessionId(accessToken),
//                "access",
//                -60_000L
//        );
//
//        mockMvc.perform(get("/api/v1/auth/me")
//                        .header("Authorization", "Bearer " + expiredAccessToken))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void refreshTokenHashMismatchCannotRefresh() throws Exception {
//        JsonNode login = login();
//        String accessToken = login.at("/data/accessToken").asText();
//        String mismatchedRefreshToken = token(
//                jwtTokenService.parseUserId(accessToken),
//                "admin",
//                jwtTokenService.parseTenantId(accessToken),
//                jwtTokenService.parseSessionId(accessToken),
//                "refresh",
//                60_000L
//        );
//
//        assertThrows(IllegalArgumentException.class,
//                () -> authenticationSessionService.refresh(mismatchedRefreshToken));
//    }
//
//    @Test
//    void expiredRefreshTokenCannotRefresh() throws Exception {
//        JsonNode login = login();
//        String accessToken = login.at("/data/accessToken").asText();
//        String refreshToken = login.at("/data/refreshToken").asText();
//        expireRefreshToken(jwtTokenService.parseTenantId(accessToken), jwtTokenService.parseSessionId(accessToken));
//
//        assertThrows(IllegalArgumentException.class,
//                () -> authenticationSessionService.refresh(refreshToken));
//    }
//
//    @Test
//    void refreshSuccessRotatesOldRefreshTokenImmediately() throws Exception {
//        JsonNode login = login();
//        String oldRefreshToken = login.at("/data/refreshToken").asText();
//
//        JsonNode refreshed = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/refresh")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.accessToken").isString())
//                .andExpect(jsonPath("$.data.refreshToken").isString())
//                .andReturn().getResponse().getContentAsString());
//
//        String newRefreshToken = refreshed.at("/data/refreshToken").asText();
//        authenticationSessionService.refresh(newRefreshToken);
//        assertThrows(IllegalArgumentException.class,
//                () -> authenticationSessionService.refresh(oldRefreshToken));
//    }
//
//    @Test
//    void logoutRevokesSessionAndOldAccessTokenCannotContinue() throws Exception {
//        JsonNode login = login();
//        String accessToken = login.at("/data/accessToken").asText();
//
//        mockMvc.perform(post("/api/v1/auth/logout")
//                        .header("Authorization", "Bearer " + accessToken))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(get("/api/v1/auth/me")
//                        .header("Authorization", "Bearer " + accessToken))
//                .andExpect(status().isUnauthorized());
//    }
//
//    private JsonNode login() throws Exception {
//        String body = mockMvc.perform(post("/api/v1/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
//                .andExpect(status().isOk())
//                .andReturn().getResponse().getContentAsString();
//        return objectMapper.readTree(body);
//    }
//
//    private void expireRefreshToken(long tenantId, String sessionId) {
//        TenantContext.runAsPlatform(() -> {
//            AuthSessionEntity session = authSessionMapper.selectOneByQuery(QueryWrapper.create()
//                    .where("tenant_id", tenantId)
//                    .and("session_id", sessionId));
//            session.setRefreshExpireAt(LocalDateTime.now().minusSeconds(1));
//            session.setUpdateTime(LocalDateTime.now());
//            authSessionMapper.update(session);
//            return null;
//        });
//    }
//
//    private String token(String userId, String username, long tenantId, String sessionId, String tokenType, long ttlMs) {
//        Date now = new Date();
//        return Jwts.builder()
//                .setId(UUID.randomUUID().toString())
//                .claim("uid", userId)
//                .claim("username", username)
//                .claim("tid", String.valueOf(tenantId))
//                .claim("sid", sessionId)
//                .claim("token_type", tokenType)
//                .setIssuedAt(now)
//                .setExpiration(new Date(now.getTime() + ttlMs))
//                .signWith(secretKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    private SecretKey secretKey() {
//        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
//        if (bytes.length < 32) {
//            byte[] padded = new byte[32];
//            System.arraycopy(bytes, 0, padded, 0, bytes.length);
//            bytes = padded;
//        }
//        return Keys.hmacShaKeyFor(bytes);
//    }
}
