package top.kx.heartbeat.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.infrastructure.event.ReliableWorkflowEventService;
import top.kx.heartbeat.infrastructure.persistence.entity.event.FlowWaitStateDO;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@ActiveProfiles("local")
class OutboxInboxIdempotencyTest {

    @Autowired
    private ReliableWorkflowEventService eventService;

    @Test
    void duplicateInboxEventDoesNotResumeWaitStateTwice() {
        TenantContext.setTenantId(1L);
        String correlationKey = "corr-" + System.nanoTime();
        eventService.createApprovalWait(1L, "approval-node", correlationKey, "{\"status\":\"WAITING\"}");

        assertTrue(eventService.consumeOnce("flow-resume", "event-" + correlationKey, correlationKey,
                "{\"status\":\"APPROVED\"}"));
        assertFalse(eventService.consumeOnce("flow-resume", "event-" + correlationKey, correlationKey,
                "{\"status\":\"APPROVED_AGAIN\"}"));

        FlowWaitStateDO wait = eventService.findWait(correlationKey);
        assertEquals("RESUMED", wait.getStatus());
        assertEquals("{\"status\":\"APPROVED\"}", wait.getPayloadJson());
    }
}
