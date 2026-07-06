package top.kx.heartbeat.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@ActiveProfiles("local")
class TypedAdminApplicationServiceTest {

    @Autowired
    private PlatformAdministrationService platformAdministrationService;

    @Test
    void typedServicesManageDepartmentsConfigurationsAndSocialProviders() {
        String suffix = String.valueOf(System.nanoTime());

        Map<String, Object> dept = new LinkedHashMap<>();
        dept.put("name", "Typed Dept " + suffix);
        dept.put("code", "typed_dept_" + suffix);
        dept.put("status", "ENABLED");
        RecordResponse createdDept = platformAdministrationService.createDepartment(dept);
        assertTrue(String.valueOf(createdDept.get("id")).length() > 0);
        assertFalse(platformAdministrationService.listDepartments().isEmpty());

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("key", "typed.config." + suffix);
        config.put("name", "Typed Config");
        config.put("value", "enabled");
        config.put("status", "ENABLED");
        RecordResponse createdConfig = platformAdministrationService.createConfiguration(config);
        assertEquals("typed.config." + suffix, createdConfig.get("key"));

        Map<String, Object> provider = new LinkedHashMap<>();
        provider.put("provider", "TYPED_" + suffix);
        provider.put("name", "Typed Provider");
        provider.put("enabled", true);
        provider.put("status", "ENABLED");
        RecordResponse createdProvider = platformAdministrationService.createSocialProvider(provider);
        assertEquals("TYPED_" + suffix, createdProvider.get("provider"));
    }

    @Test
    void typedReadOnlyResourcesAreExplicitlyAvailable() {
        platformAdministrationService.listTenants();
        platformAdministrationService.listPosts();
        platformAdministrationService.listDictTypes();
        platformAdministrationService.listDictData();
        platformAdministrationService.listNotices();
        platformAdministrationService.listOperationLogs();
        platformAdministrationService.listOnlineSessions();
        platformAdministrationService.listOauthClients();
    }
}
