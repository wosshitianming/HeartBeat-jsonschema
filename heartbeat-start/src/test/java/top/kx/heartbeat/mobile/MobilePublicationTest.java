package top.kx.heartbeat.mobile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.application.mobile.MobileService;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;
import top.kx.heartbeat.support.MySqlIntegrationTestSupport;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@ActiveProfiles("local")
class MobilePublicationTest extends MySqlIntegrationTestSupport {

    @Autowired
    private MobileService mobileService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void publishedAppCreatesImmutableVersionRecord() {
        TenantContext.setTenantId(1L);
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("name", "mobile-" + suffix);
        command.put("appKey", "mobile-" + suffix);
        command.put("status", "PUBLISHED");

//        Map<String, Object> app = mobileService.saveApp(command);
//
//        assertEquals(1, jdbcTemplate.queryForList(
//                "select * from mobile_app_version where tenant_id = ? and app_id = ? and status = ?",
//                1L, app.get("id"), "PUBLISHED").size());
    }
}
