package top.kx.heartbeat.infrastructure.flow.flowable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import top.kx.heartbeat.application.flow.runtime.*;
import top.kx.heartbeat.domain.flow.model.FlowNode;
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;
import top.kx.heartbeat.support.MySqlIntegrationTestSupport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowExternalIoCommandDispatcherTest extends MySqlIntegrationTestSupport {

    @Mock
    private FlowableVariableCodec variableCodec;
    @Mock
    private ObjectProvider<FlowableRuntimeService> heartbeatRuntimeServiceProvider;
    @Mock
    private ObjectProvider<org.flowable.engine.RuntimeService> flowableRuntimeServiceProvider;
    @Mock
    private FlowableRuntimeService heartbeatRuntimeService;
    @Mock
    private org.flowable.engine.RuntimeService flowableRuntimeService;
    @Mock
    private DelegateExecution prepareExecution;
    @Mock
    private DelegateExecution waitExecution;

    private FlowExternalIoCommandDispatcher dispatcher;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        createCommandTable();
        dispatcher = new FlowExternalIoCommandDispatcher();
        ReflectionTestUtils.setField(dispatcher, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(dispatcher, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(dispatcher, "secretCryptoService", new SecretCryptoService("test-key"));
        ReflectionTestUtils.setField(dispatcher, "variableCodec", variableCodec);
        ReflectionTestUtils.setField(dispatcher, "heartbeatRuntimeService", heartbeatRuntimeServiceProvider);
        ReflectionTestUtils.setField(dispatcher, "flowableRuntimeService", flowableRuntimeServiceProvider);
        when(heartbeatRuntimeServiceProvider.getObject()).thenReturn(heartbeatRuntimeService);
        when(flowableRuntimeServiceProvider.getObject()).thenReturn(flowableRuntimeService);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void workerLifecycleUsesEncryptedCommandsFencingAndTrustedResumeIdentity() {
        FlowNode node = new FlowNode();
        node.setId("http-1");
        node.setType("action.http.request");
        node.setVersion("1.0.0");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("url", "https://internal.example/orders");
        config.put("method", "GET");
        node.setConfig(config);
        Map<String, Object> payload = Collections.singletonMap("orderId", "HB-101");
        when(variableCodec.readTenantId(prepareExecution)).thenReturn("7");
        when(variableCodec.readRunId(prepareExecution)).thenReturn("101");
        when(variableCodec.resolveExecutorId(prepareExecution, node.getType()))
                .thenReturn("builtin:http.request");
        when(prepareExecution.getProcessInstanceId()).thenReturn("pi-1");
        when(prepareExecution.getId()).thenReturn("ex-prepare");
        when(prepareExecution.getVariable("hbFlowVersionId")).thenReturn("33");

        dispatcher.dispatch(prepareExecution, node, payload);

        Long commandId = jdbcTemplate.queryForObject(
                "SELECT id FROM hb_flow_io_command", Long.class);
        String storedRequest = jdbcTemplate.queryForObject(
                "SELECT request_json FROM hb_flow_io_command WHERE id = ?", String.class, commandId);
        assertNotNull(storedRequest);
        assertFalse(storedRequest.contains("internal.example"));
        assertFalse(storedRequest.contains("HB-101"));

        when(waitExecution.getVariable(FlowableVariableCodec.IO_COMMAND_ID)).thenReturn(commandId);
        when(variableCodec.readTenantId(waitExecution)).thenReturn("7");
        when(variableCodec.readRunId(waitExecution)).thenReturn("101");
        when(waitExecution.getProcessInstanceId()).thenReturn("pi-1");
        when(waitExecution.getId()).thenReturn("ex-wait");
        dispatcher.bindWaitExecution(waitExecution, "wait-1");

        TenantContext.setTenantId(7L);
        FlowExternalIoClaimRequest claim = new FlowExternalIoClaimRequest();
        claim.setWorkerId("worker-a");
        claim.setWorkerTopics(Collections.singletonList("flow-io-http"));
        FlowExternalIoCommandView leased = dispatcher.claim(claim).orElseThrow(AssertionError::new);
        assertEquals("CALL_PREPARED", leased.getStatus());
        assertEquals("https://internal.example/orders",
                ((Map<?, ?>) leased.getRequest().get("config")).get("url"));
        assertNotNull(leased.getLeaseToken());

        FlowExternalIoStartedRequest stale = new FlowExternalIoStartedRequest();
        stale.setWorkerId("worker-a");
        stale.setLeaseToken("stale-token");
        assertThrows(IllegalStateException.class,
                () -> dispatcher.markCallStarted(leased.getCommandId(), stale));

        FlowExternalIoStartedRequest started = new FlowExternalIoStartedRequest();
        started.setWorkerId("worker-a");
        started.setLeaseToken(leased.getLeaseToken());
        assertEquals("CALL_STARTED",
                dispatcher.markCallStarted(leased.getCommandId(), started).getStatus());

        when(flowableRuntimeService.getVariableLocal("ex-wait", "hbWaitInstanceId"))
                .thenReturn("wait-1");
        FlowExternalIoCompletionRequest completion = new FlowExternalIoCompletionRequest();
        completion.setWorkerId("worker-a");
        completion.setLeaseToken(leased.getLeaseToken());
        completion.setOutcomeStatus("SUCCESS");
        completion.setOutput(Collections.singletonMap("statusCode", 200));
        assertEquals("SUCCEEDED",
                dispatcher.complete(leased.getCommandId(), completion).getStatus());

        ArgumentCaptor<FlowResumeCommand> resume = ArgumentCaptor.forClass(FlowResumeCommand.class);
        verify(heartbeatRuntimeService).resume(resume.capture());
        assertEquals("7", resume.getValue().getTenantId());
        assertEquals("101", resume.getValue().getRunId());
        assertEquals("pi-1", resume.getValue().getEngineInstanceId());
        assertEquals("ex-wait", resume.getValue().getExecutionId());
        assertEquals("wait-1", resume.getValue().getWaitInstanceId());
        assertEquals("io.node.completed", resume.getValue().getMessageName());
        assertEquals(200, resume.getValue().getPayload().get("statusCode"));
    }

    private void createCommandTable() {
        jdbcTemplate.execute("CREATE TABLE hb_flow_io_command ("
                + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, "
                + "run_id BIGINT NOT NULL, flow_version_id BIGINT, node_id VARCHAR(128) NOT NULL, "
                + "node_type VARCHAR(128), node_version VARCHAR(32), executor_id VARCHAR(128), "
                + "node_config_json LONGTEXT, engine_instance_id VARCHAR(128), execution_id VARCHAR(128), "
                + "wait_instance_id VARCHAR(128), command_type VARCHAR(64) NOT NULL, "
                + "worker_topic VARCHAR(128) NOT NULL, message_name VARCHAR(128) NOT NULL, "
                + "correlation_key VARCHAR(128), idempotency_key VARCHAR(128) NOT NULL, "
                + "request_json LONGTEXT, response_json LONGTEXT, status VARCHAR(32) NOT NULL, "
                + "attempt_no INT NOT NULL DEFAULT 0, max_attempts INT NOT NULL DEFAULT 1, "
                + "next_attempt_at TIMESTAMP, lease_owner VARCHAR(128), lease_until TIMESTAMP, "
                + "lease_token VARCHAR(64), lease_version BIGINT NOT NULL DEFAULT 0, "
                + "external_call_policy VARCHAR(32) NOT NULL DEFAULT 'MANUAL_ONLY', timeout_at TIMESTAMP, "
                + "call_started_at TIMESTAMP, completed_at TIMESTAMP, result_applied_at TIMESTAMP, "
                + "error_code VARCHAR(64), error_message TEXT, create_time TIMESTAMP NOT NULL, "
                + "update_time TIMESTAMP NOT NULL, create_by BIGINT NOT NULL, update_by BIGINT NOT NULL, "
                + "UNIQUE (tenant_id, idempotency_key), UNIQUE (tenant_id, correlation_key))");
    }
}
