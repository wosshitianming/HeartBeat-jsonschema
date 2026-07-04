package top.kx.heartbeat.mp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.application.mp.MpService;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@ActiveProfiles("local")
class MpCredentialSecurityTest {

    @Autowired
    private MpService mpService;

    @Test
    void accountSecretsAreMaskedInResponses() {
        TenantContext.setTenantId(1L);
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("name", "mp-" + suffix);
        command.put("appId", "app-" + suffix);
        command.put("appSecret", "secret-value");
        command.put("token", "token-value");
        command.put("aesKey", "aes-value");

        Map<String, Object> saved = mpService.saveAccount(command);

        assertEquals("******", saved.get("appSecret"));
        assertEquals("******", saved.get("token"));
        assertEquals("******", saved.get("aesKey"));
    }
}
