package top.kx.heartbeat.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.kx.heartbeat.domain.auth.AuthTokenPayload;
import top.kx.heartbeat.infrastructure.security.JwtProperties;
import top.kx.heartbeat.infrastructure.security.JwtTokenService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    @Test
    void rejectsMissingOrShortSigningSecrets() {
        assertThrows(IllegalStateException.class,
                () -> serviceWithSecret(null).issueTokens("42", "alice", "7", "session-1"));
        assertThrows(IllegalStateException.class,
                () -> serviceWithSecret("short-secret").issueTokens("42", "alice", "7", "session-1"));
    }

    @Test
    void acceptsSigningSecretWithAtLeastThirtyTwoBytes() {
        JwtTokenService service = serviceWithSecret("0123456789abcdef0123456789abcdef");

        AuthTokenPayload tokens = service.issueTokens("42", "alice", "7", "session-1");

        assertEquals("42", service.parseUserId(tokens.getAccessToken()));
        assertEquals(7L, service.parseTenantId(tokens.getAccessToken()));
        assertEquals("session-1", service.parseSessionId(tokens.getAccessToken()));
    }

    private JwtTokenService serviceWithSecret(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        JwtTokenService service = new JwtTokenService();
        ReflectionTestUtils.setField(service, "properties", properties);
        return service;
    }
}
