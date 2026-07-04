package top.kx.heartbeat.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.kx.heartbeat.domain.platform.iam.UserId;
import top.kx.heartbeat.domain.platform.tenant.TenantId;
import top.kx.heartbeat.infrastructure.tenant.HeartbeatTenantFactory;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import static org.junit.jupiter.api.Assertions.*;

class TenantIsolationSecurityTest {

    private final HeartbeatTenantFactory tenantFactory = new HeartbeatTenantFactory();

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void tenantContextFailsClosedWhenNoTenantIsAuthenticated() {
        TenantContext.clear();

        assertNull(TenantContext.getTenantId());
        assertThrows(IllegalStateException.class, TenantContext::getRequiredTenantId);
        assertThrows(IllegalStateException.class, tenantFactory::getTenantIds);
    }

    @Test
    void tenantContextAcceptsOnlyPositiveLongTenantIds() {
        TenantContext.setTenantId(42L);

        assertEquals(Long.valueOf(42L), TenantContext.getTenantId());
        assertEquals(42L, TenantContext.getRequiredTenantId());
        assertArrayEquals(new Object[]{42L}, tenantFactory.getTenantIds());

        assertThrows(IllegalArgumentException.class, () -> TenantContext.setTenantId(0L));
        assertThrows(IllegalArgumentException.class, () -> TenantContext.setTenantId(-1L));
        assertThrows(IllegalArgumentException.class, () -> TenantContext.setTenantId("tenant-a"));
    }

    @Test
    void platformScopeMustBeExplicitAndTemporarilyDisablesTenantFiltering() {
        TenantContext.setTenantId(7L);

        String result = TenantContext.runAsPlatform(() -> {
            assertNull(TenantContext.getTenantId());
            assertArrayEquals(new Object[0], tenantFactory.getTenantIds());
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(Long.valueOf(7L), TenantContext.getTenantId());
        assertArrayEquals(new Object[]{7L}, tenantFactory.getTenantIds());
    }

    @Test
    void platformIdsRejectZeroAndNegativeValues() {
        assertEquals(1L, TenantId.of(1L).value());
        assertEquals(1L, UserId.of(1L).value());
        assertThrows(IllegalArgumentException.class, () -> TenantId.of(0L));
        assertThrows(IllegalArgumentException.class, () -> UserId.of(-1L));
    }
}
