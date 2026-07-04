package top.kx.heartbeat.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.application.workflow.WorkflowService;
import top.kx.heartbeat.infrastructure.persistence.mapper.event.SysOutboxEventMapper;
import top.kx.heartbeat.infrastructure.persistence.query.QueryWrapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "heartbeat.security.dev-auto-login=false",
        "heartbeat.security.dev-header-enabled=false"
})
@ActiveProfiles("local")
class WorkflowApprovalIntegrationTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private SysOutboxEventMapper outboxEventMapper;

    @Test
    void approvalCompletesTaskAndWritesOutboxEvent() {
        TenantContext.setTenantId(1L);
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> definitionCommand = new LinkedHashMap<>();
        definitionCommand.put("name", "Approval " + suffix);
        definitionCommand.put("definitionKey", "approval_" + suffix);
        definitionCommand.put("formSchema", formSchema());

        Map<String, Object> definition = workflowService.createDefinition(definitionCommand);
        workflowService.deployDefinition(String.valueOf(definition.get("id")));

        Map<String, Object> startCommand = new LinkedHashMap<>();
        startCommand.put("title", "Approval Instance " + suffix);
        startCommand.put("businessKey", "biz-" + suffix);
        workflowService.startInstance(String.valueOf(definition.get("id")), startCommand);

        List<Map<String, Object>> tasks = workflowService.listTodoTasks();
        assertFalse(tasks.isEmpty());
        Map<String, Object> task = tasks.get(0);
        Map<String, Object> approveCommand = new LinkedHashMap<>();
        approveCommand.put("comment", "ok");
        Map<String, Object> completed = workflowService.approve(String.valueOf(task.get("id")), approveCommand);

        assertEquals("APPROVED", completed.get("status"));
        long outboxCount = outboxEventMapper.selectCountByQuery(QueryWrapper.create()
                .where("event_type", "WORKFLOW_TASK_COMPLETED")
                .and("aggregate_id", String.valueOf(task.get("id"))));
        assertTrue(outboxCount > 0);
    }

    private Map<String, Object> formSchema() {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("name", "审批");
        task.put("assigneeId", "1");
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("userTasks", java.util.Collections.singletonList(task));
        return schema;
    }
}
