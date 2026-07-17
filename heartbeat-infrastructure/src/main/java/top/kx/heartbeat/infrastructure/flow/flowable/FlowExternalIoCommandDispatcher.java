package top.kx.heartbeat.infrastructure.flow.flowable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.flow.runtime.*;
import top.kx.heartbeat.domain.flow.model.FlowNode;
import top.kx.heartbeat.domain.flow.model.FlowNodeRunStatus;
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Durable dispatcher and worker state machine for Flow external I/O nodes.
 *
 * <p>This component never performs the remote call. It only freezes a command, leases it to a
 * worker and resumes the matching Flowable execution after a durable terminal result exists.</p>
 */
@Service
public class FlowExternalIoCommandDispatcher implements FlowExternalIoWorkerPort {

    static final String MESSAGE_NAME = "io.node.completed";

    private static final String PENDING = "PENDING";
    private static final String LOCKED = "LOCKED";
    private static final String CALL_PREPARED = "CALL_PREPARED";
    private static final String CALL_STARTED = "CALL_STARTED";
    private static final String SUCCEEDED = "SUCCEEDED";
    private static final String FAILED_RETRYABLE = "FAILED_RETRYABLE";
    private static final String FAILED_FINAL = "FAILED_FINAL";
    private static final String FAILED_AMBIGUOUS = "FAILED_AMBIGUOUS";
    private static final String RECONCILING = "RECONCILING";
    private static final String MANUAL_REQUIRED = "MANUAL_REQUIRED";
    private static final String TIMEOUT = "TIMEOUT";
    private static final String CANCELED = "CANCELED";
    private static final Set<String> TERMINAL = Collections.unmodifiableSet(new LinkedHashSet<>(
            Arrays.asList(SUCCEEDED, FAILED_FINAL, TIMEOUT, CANCELED)));

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private SecretCryptoService secretCryptoService;

    @Resource
    private FlowableVariableCodec variableCodec;

    @Autowired
    private ObjectProvider<FlowableRuntimeService> heartbeatRuntimeService;

    @Autowired
    private ObjectProvider<org.flowable.engine.RuntimeService> flowableRuntimeService;

    @Resource
    private FlowExternalIoCommandCancellationService cancellationService;

    public boolean supports(FlowNode node) {
        String type = node == null ? null : node.getType();
        return StringUtils.containsIgnoreCase(type, ".http.")
                || StringUtils.containsIgnoreCase(type, ".mysql.")
                || StringUtils.containsIgnoreCase(type, ".redis.")
                || StringUtils.containsIgnoreCase(type, ".mq.")
                || StringUtils.containsIgnoreCase(type, ":http.")
                || StringUtils.containsIgnoreCase(type, ":mysql.")
                || StringUtils.containsIgnoreCase(type, ":redis.")
                || StringUtils.containsIgnoreCase(type, ":mq.");
    }

    /**
     * Freeze an immutable worker command in the same transaction that advances Flowable.
     */
    @Transactional
    public void dispatch(DelegateExecution execution, FlowNode node, Map<String, Object> payload) {
        long tenantId = positiveLong(variableCodec.readTenantId(execution), "tenantId");
        long runId = positiveLong(variableCodec.readRunId(execution), "runId");
        String executorId = variableCodec.resolveExecutorId(execution, node.getType());
        String identity = tenantId + ":" + runId + ":" + execution.getProcessInstanceId()
                + ":" + execution.getId() + ":" + node.getId();
        String idempotencyKey = "flow-io-" + sha256(identity);
        String correlationKey = "flow-io-result-" + sha256(identity);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("nodeId", node.getId());
        request.put("nodeType", node.getType());
        request.put("nodeVersion", node.getVersion());
        request.put("executorId", executorId);
        request.put("config", copy(node.getConfig()));
        request.put("payload", copy(payload));
        request.put("idempotencyKey", idempotencyKey);

        Map<String, Object> row = findByIdempotency(tenantId, idempotencyKey);
        if (row == null) {
            try {
                long commandId = insertCommand(execution, node, executorId, tenantId, runId,
                        idempotencyKey, correlationKey, request);
                row = load(tenantId, commandId, false);
            } catch (DuplicateKeyException ex) {
                row = findByIdempotency(tenantId, idempotencyKey);
                if (row == null) throw ex;
            }
        }
        validateDispatchIdentity(row, execution, node, executorId, runId);
        execution.setVariable(FlowableVariableCodec.IO_COMMAND_ID, text(row.get("id")));
        execution.setVariable(FlowableVariableCodec.IO_CORRELATION_KEY, text(row.get("correlation_key")));
        execution.setVariable(FlowableVariableCodec.IO_MESSAGE_NAME, MESSAGE_NAME);
        execution.setVariable(FlowableVariableCodec.NEXT_PORTS, new ArrayList<>());
        execution.setVariable("hbLastNodeStatus", FlowNodeRunStatus.WAITING.getCode());
        execution.setVariable("hbLastErrorCode", null);
        execution.setVariable("hbLastErrorMessage", null);
    }

    @Transactional
    public void bindWaitExecution(DelegateExecution execution, String waitInstanceId) {
        Object commandValue = execution.getVariable(FlowableVariableCodec.IO_COMMAND_ID);
        if (commandValue == null) return;
        long tenantId = positiveLong(variableCodec.readTenantId(execution), "tenantId");
        long runId = positiveLong(variableCodec.readRunId(execution), "runId");
        long commandId = positiveLong(String.valueOf(commandValue), "ioCommandId");
        int changed = jdbcTemplate.update("UPDATE hb_flow_io_command SET execution_id = ?, wait_instance_id = ?, "
                        + "update_time = ? WHERE tenant_id = ? AND id = ? AND run_id = ? AND engine_instance_id = ? "
                        + "AND status NOT IN ('SUCCEEDED','FAILED_FINAL','TIMEOUT','CANCELED')",
                execution.getId(), waitInstanceId, Timestamp.from(Instant.now()), tenantId, commandId, runId,
                execution.getProcessInstanceId());
        if (changed != 1) {
            throw new IllegalStateException("外部 I/O 等待 execution 绑定失败: " + commandId);
        }
        execution.setVariableLocal(FlowableVariableCodec.IO_CORRELATION_KEY,
                execution.getVariable(FlowableVariableCodec.IO_CORRELATION_KEY));
        execution.setVariableLocal(FlowableVariableCodec.IO_MESSAGE_NAME, MESSAGE_NAME);
    }

    /**
     * Result delegate: apply a terminal row, but never create or re-dispatch a command.
     */
    @Transactional
    public NodeExecutionOutcome applyResult(DelegateExecution execution, FlowNode node) {
        long tenantId = positiveLong(variableCodec.readTenantId(execution), "tenantId");
        long runId = positiveLong(variableCodec.readRunId(execution), "runId");
        long commandId = positiveLong(text(execution.getVariable(FlowableVariableCodec.IO_COMMAND_ID)), "ioCommandId");
        Map<String, Object> row = load(tenantId, commandId, true);
        validateResultIdentity(row, execution, node, runId);
        String status = text(row.get("status"));
        if (!TERMINAL.contains(status)) {
            throw new IllegalStateException("外部 I/O 命令尚未形成最终结果: " + commandId + " (" + status + ")");
        }
        Map<String, Object> output = decryptJson(row.get("response_json"));
        NodeExecutionOutcome outcome = new NodeExecutionOutcome();
        outcome.setOutput(output);
        outcome.setErrorCode(text(row.get("error_code")));
        outcome.setErrorMessage(text(row.get("error_message")));
        if (SUCCEEDED.equals(status)) {
            outcome.setStatus(FlowNodeRunStatus.SUCCESS.getCode());
            outcome.setNextPorts(Collections.singletonList("out"));
        } else if (TIMEOUT.equals(status)) {
            outcome.setStatus(FlowNodeRunStatus.FAILED.getCode());
            outcome.setNextPorts(Collections.singletonList("error"));
        } else {
            outcome.setStatus(FlowNodeRunStatus.FAILED.getCode());
            outcome.setNextPorts(Collections.singletonList("error"));
        }
        jdbcTemplate.update("UPDATE hb_flow_io_command SET result_applied_at = COALESCE(result_applied_at, ?), "
                        + "update_time = ? WHERE tenant_id = ? AND id = ?",
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), tenantId, commandId);
        execution.removeVariable(FlowableVariableCodec.IO_COMMAND_ID);
        execution.removeVariable(FlowableVariableCodec.IO_CORRELATION_KEY);
        execution.removeVariable(FlowableVariableCodec.IO_MESSAGE_NAME);
        return outcome;
    }

    @Override
    @Transactional
    public Optional<FlowExternalIoCommandView> claim(FlowExternalIoClaimRequest request) {
        String workerId = requiredWorker(request == null ? null : request.getWorkerId());
        List<String> topics = topics(request == null ? null : request.getWorkerTopics());
        int leaseSeconds = boundedSeconds(request == null ? 0 : request.getLeaseSeconds(), 60, 5, 3600);
        long tenantId = TenantContext.getRequiredTenantId();
        Instant now = Instant.now();
        String placeholders = topics.stream().map(item -> "?").collect(Collectors.joining(","));
        String sql = "SELECT * FROM hb_flow_io_command WHERE tenant_id = ? "
                + "AND worker_topic IN (" + placeholders + ") "
                + "AND status IN ('PENDING','FAILED_RETRYABLE') "
                + "AND wait_instance_id IS NOT NULL "
                + "AND (next_attempt_at IS NULL OR next_attempt_at <= ?) "
                + "AND (timeout_at IS NULL OR timeout_at > ?) AND attempt_no < max_attempts "
                + "ORDER BY COALESCE(next_attempt_at, create_time), id LIMIT 1 FOR UPDATE";
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.addAll(topics);
        args.add(Timestamp.from(now));
        args.add(Timestamp.from(now));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args.toArray());
        if (rows.isEmpty()) return Optional.empty();
        Map<String, Object> row = rows.get(0);
        long commandId = number(row.get("id"));
        Instant leaseUntil = leaseUntil(row, now, leaseSeconds);
        String leaseToken = UUID.randomUUID().toString();
        int changed = jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, attempt_no = attempt_no + 1, "
                        + "lease_owner = ?, lease_until = ?, lease_token = ?, lease_version = lease_version + 1, update_time = ? "
                        + "WHERE tenant_id = ? AND id = ? AND status = ?",
                CALL_PREPARED, workerId, Timestamp.from(leaseUntil), leaseToken, Timestamp.from(now),
                tenantId, commandId, text(row.get("status")));
        if (changed != 1) return Optional.empty();
        return Optional.of(toView(load(tenantId, commandId, false)));
    }

    @Override
    @Transactional
    public FlowExternalIoCommandView markCallStarted(String commandId, FlowExternalIoStartedRequest request) {
        long tenantId = TenantContext.getRequiredTenantId();
        long id = positiveLong(commandId, "commandId");
        String workerId = requiredWorker(request == null ? null : request.getWorkerId());
        String leaseToken = requiredLeaseToken(request == null ? null : request.getLeaseToken());
        int leaseSeconds = boundedSeconds(request == null ? 0 : request.getLeaseSeconds(), 60, 5, 3600);
        Map<String, Object> row = load(tenantId, id, true);
        requireLease(row, workerId, leaseToken);
        requireActiveLease(row);
        String status = text(row.get("status"));
        if (CALL_STARTED.equals(status)) return toView(row);
        if (!CALL_PREPARED.equals(status) && !LOCKED.equals(status)) {
            throw new IllegalStateException("外部 I/O 命令不能从 " + status + " 进入 CALL_STARTED");
        }
        Instant now = Instant.now();
        Instant leaseUntil = leaseUntil(row, now, leaseSeconds);
        int changed = jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, call_started_at = COALESCE(call_started_at, ?), "
                        + "lease_until = ?, update_time = ? WHERE tenant_id = ? AND id = ? "
                        + "AND lease_owner = ? AND lease_token = ? AND status IN ('LOCKED','CALL_PREPARED')",
                CALL_STARTED, Timestamp.from(now), Timestamp.from(leaseUntil),
                Timestamp.from(now), tenantId, id, workerId, leaseToken);
        if (changed != 1) throw new IllegalStateException("外部 I/O CALL_STARTED fencing 校验失败");
        return toView(load(tenantId, id, false));
    }

    @Override
    @Transactional
    public FlowExternalIoCommandView renewLease(String commandId, FlowExternalIoStartedRequest request) {
        long tenantId = TenantContext.getRequiredTenantId();
        long id = positiveLong(commandId, "commandId");
        String workerId = requiredWorker(request == null ? null : request.getWorkerId());
        String leaseToken = requiredLeaseToken(request == null ? null : request.getLeaseToken());
        int leaseSeconds = boundedSeconds(request == null ? 0 : request.getLeaseSeconds(), 60, 5, 3600);
        Map<String, Object> row = load(tenantId, id, true);
        requireLease(row, workerId, leaseToken);
        requireActiveLease(row);
        String status = text(row.get("status"));
        if (!CALL_PREPARED.equals(status) && !CALL_STARTED.equals(status)) {
            throw new IllegalStateException("当前状态不允许续租: " + status);
        }
        Instant now = Instant.now();
        Instant leaseUntil = leaseUntil(row, now, leaseSeconds);
        int changed = jdbcTemplate.update("UPDATE hb_flow_io_command SET lease_until = ?, update_time = ? "
                        + "WHERE tenant_id = ? AND id = ? AND lease_owner = ? AND lease_token = ? AND status = ?",
                Timestamp.from(leaseUntil), Timestamp.from(now), tenantId, id,
                workerId, leaseToken, status);
        if (changed != 1) throw new IllegalStateException("外部 I/O 续租 fencing 校验失败");
        return toView(load(tenantId, id, false));
    }

    @Override
    @Transactional
    public FlowExternalIoCommandView complete(String commandId, FlowExternalIoCompletionRequest request) {
        long tenantId = TenantContext.getRequiredTenantId();
        long id = positiveLong(commandId, "commandId");
        String workerId = requiredWorker(request == null ? null : request.getWorkerId());
        String leaseToken = requiredLeaseToken(request == null ? null : request.getLeaseToken());
        String requestedStatus = completionStatus(request == null ? null : request.getOutcomeStatus());
        Map<String, Object> row = load(tenantId, id, true);
        String currentStatus = text(row.get("status"));
        if (TERMINAL.contains(currentStatus)) {
            if (!compatibleCompletion(currentStatus, requestedStatus)) {
                throw new IllegalStateException("命令已以不同结果完成: " + currentStatus);
            }
            return toView(row);
        }
        if (!CALL_STARTED.equals(currentStatus)) {
            throw new IllegalStateException("外部调用尚未标记 CALL_STARTED: " + currentStatus);
        }
        requireLease(row, workerId, leaseToken);
        requireActiveLease(row);
        Instant now = Instant.now();
        Instant timeoutAt = instant(row.get("timeout_at"));
        String finalStatus = requestedStatus;
        if (timeoutAt != null && !timeoutAt.isAfter(now)) {
            finalStatus = TIMEOUT;
        }
        if (FAILED_RETRYABLE.equals(finalStatus)
                && number(row.get("attempt_no")) < number(row.get("max_attempts"))) {
            int delay = boundedSeconds(request.getRetryDelaySeconds(), 5, 1, 86400);
            jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, next_attempt_at = ?, "
                            + "lease_owner = NULL, lease_until = NULL, lease_token = NULL, error_code = ?, error_message = ?, update_time = ? "
                            + "WHERE tenant_id = ? AND id = ?",
                    FAILED_RETRYABLE, Timestamp.from(now.plusSeconds(delay)), truncate(request.getErrorCode(), 64),
                    truncate(request.getErrorMessage(), 2000), Timestamp.from(now), tenantId, id);
            return toView(load(tenantId, id, false));
        }
        if (FAILED_RETRYABLE.equals(finalStatus)) finalStatus = FAILED_FINAL;
        Map<String, Object> output = request == null ? new LinkedHashMap<>() : copy(request.getOutput());
        persistTerminal(row, finalStatus, output,
                request == null ? null : request.getErrorCode(),
                request == null ? null : request.getErrorMessage(), now);
        resumeTerminal(row, output);
        return toView(load(tenantId, id, false));
    }

    @Override
    @Transactional
    public FlowExternalIoCommandView resolve(String commandId, FlowExternalIoResolutionRequest request) {
        if (request == null) throw new IllegalArgumentException("resolution request 不能为空");
        long tenantId = TenantContext.getRequiredTenantId();
        long id = positiveLong(commandId, "commandId");
        Map<String, Object> row = load(tenantId, id, true);
        String currentStatus = text(row.get("status"));
        String requestedStatus = StringUtils.upperCase(StringUtils.trimToEmpty(request.getOutcomeStatus()), Locale.ROOT);
        if (TERMINAL.contains(currentStatus)) {
            String normalized = "RETRY".equals(requestedStatus) ? requestedStatus : completionStatus(requestedStatus);
            if (!currentStatus.equals(normalized)) {
                throw new IllegalStateException("命令已以不同结果完成: " + currentStatus);
            }
            return toView(row);
        }
        if (!FAILED_AMBIGUOUS.equals(currentStatus)
                && !RECONCILING.equals(currentStatus)
                && !MANUAL_REQUIRED.equals(currentStatus)) {
            throw new IllegalStateException("当前状态不允许人工/对账终结: " + currentStatus);
        }
        Instant now = Instant.now();
        if ("RETRY".equals(requestedStatus)) {
            if (!request.isConfirmedNotExecuted()) {
                throw new IllegalArgumentException("重新投递前必须确认下游未执行");
            }
            jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, attempt_no = ?, next_attempt_at = ?, "
                            + "lease_owner = NULL, lease_until = NULL, lease_token = NULL, error_code = NULL, "
                            + "error_message = NULL, update_time = ? WHERE tenant_id = ? AND id = ?",
                    PENDING, Math.max(0, (int) number(row.get("attempt_no")) - 1), Timestamp.from(now),
                    Timestamp.from(now), tenantId, id);
            return toView(load(tenantId, id, false));
        }
        String finalStatus = completionStatus(requestedStatus);
        if (FAILED_RETRYABLE.equals(finalStatus)) {
            throw new IllegalArgumentException("状态不明命令不能未经确认直接进入自动重试");
        }
        Map<String, Object> output = copy(request.getOutput());
        persistTerminal(row, finalStatus, output, request.getErrorCode(), request.getErrorMessage(), now);
        resumeTerminal(row, output);
        return toView(load(tenantId, id, false));
    }

    /**
     * Reconcile expired leases without pretending an ambiguous side effect succeeded.
     */
    @Transactional
    public Map<String, Integer> reconcileExpiredLeases(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 500));
        Instant now = Instant.now();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM hb_flow_io_command WHERE "
                        + "((status IN ('LOCKED','CALL_PREPARED','CALL_STARTED') AND lease_until < ?) "
                        + "OR (status IN ('PENDING','FAILED_RETRYABLE','LOCKED','CALL_PREPARED','CALL_STARTED') "
                        + "AND timeout_at <= ?)) "
                        + "ORDER BY COALESCE(lease_until, timeout_at), id LIMIT ? FOR UPDATE",
                Timestamp.from(now), Timestamp.from(now), limit);
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("scanned", rows.size());
        for (Map<String, Object> row : rows) {
            String status = text(row.get("status"));
            Instant timeoutAt = instant(row.get("timeout_at"));
            if (timeoutAt != null && !timeoutAt.isAfter(now)) {
                persistTerminal(row, TIMEOUT, Collections.emptyMap(), "IO_TIMEOUT",
                        "外部 I/O 命令超过截止时间", now);
                resumeTerminal(row, Collections.emptyMap());
                increment(summary, TIMEOUT);
                continue;
            }
            if (LOCKED.equals(status) || CALL_PREPARED.equals(status)) {
                jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, attempt_no = ?, "
                                + "next_attempt_at = ?, lease_owner = NULL, lease_until = NULL, lease_token = NULL, update_time = ? "
                                + "WHERE tenant_id = ? AND id = ?",
                        PENDING, Math.max(0, (int) number(row.get("attempt_no")) - 1), Timestamp.from(now),
                        Timestamp.from(now), number(row.get("tenant_id")), number(row.get("id")));
                increment(summary, PENDING);
                continue;
            }
            String policy = StringUtils.defaultIfBlank(text(row.get("external_call_policy")), "IDEMPOTENT_RETRY");
            if ("IDEMPOTENT_RETRY".equals(policy)) {
                if (number(row.get("attempt_no")) >= number(row.get("max_attempts"))) {
                    persistTerminal(row, FAILED_FINAL, Collections.emptyMap(), "IO_RETRY_EXHAUSTED",
                            "外部调用已开始且租约过期，重试次数已耗尽", now);
                    resumeTerminal(row, Collections.emptyMap());
                    increment(summary, FAILED_FINAL);
                } else {
                    jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, next_attempt_at = ?, "
                                    + "lease_owner = NULL, lease_until = NULL, lease_token = NULL, error_code = ?, error_message = ?, update_time = ? "
                                    + "WHERE tenant_id = ? AND id = ?",
                            FAILED_RETRYABLE, Timestamp.from(now), "IO_LEASE_EXPIRED",
                            "外部调用已开始，复用原幂等键重新投递", Timestamp.from(now),
                            number(row.get("tenant_id")), number(row.get("id")));
                    increment(summary, FAILED_RETRYABLE);
                }
            } else if ("QUERY_THEN_RETRY".equals(policy)) {
                moveAmbiguous(row, RECONCILING, now,
                        "外部调用已开始且租约过期，必须先查询下游状态");
                increment(summary, RECONCILING);
            } else {
                moveAmbiguous(row, FAILED_AMBIGUOUS, now,
                        "外部调用已开始且下游不支持安全重试，需要人工确认");
                increment(summary, FAILED_AMBIGUOUS);
            }
        }
        return summary;
    }

    @Transactional
    public int cancelByRun(String tenantId, String runId, String engineInstanceId, String reason) {
        return cancellationService.cancelByRun(tenantId, runId, engineInstanceId, reason);
    }

    private long insertCommand(DelegateExecution execution, FlowNode node, String executorId,
                               long tenantId, long runId, String idempotencyKey,
                               String correlationKey, Map<String, Object> request) {
        Instant now = Instant.now();
        Map<String, Object> config = copy(node.getConfig());
        String policy = externalCallPolicy(node.getType(), config);
        int maxAttempts = maxAttempts(config, policy);
        Instant timeoutAt = now.plus(timeout(config));
        Long flowVersionId = nullablePositiveLong(text(execution.getVariable("hbFlowVersionId")));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO hb_flow_io_command (tenant_id, run_id, flow_version_id, node_id, node_type, "
                            + "node_version, executor_id, node_config_json, engine_instance_id, execution_id, "
                            + "command_type, worker_topic, message_name, correlation_key, idempotency_key, "
                            + "request_json, response_json, status, attempt_no, max_attempts, next_attempt_at, "
                            + "lease_owner, lease_until, external_call_policy, timeout_at, call_started_at, "
                            + "completed_at, result_applied_at, error_code, error_message, create_time, update_time, "
                            + "create_by, update_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            int index = 1;
            statement.setLong(index++, tenantId);
            statement.setLong(index++, runId);
            if (flowVersionId == null) statement.setObject(index++, null);
            else statement.setLong(index++, flowVersionId);
            statement.setString(index++, truncate(node.getId(), 128));
            statement.setString(index++, truncate(node.getType(), 128));
            statement.setString(index++, truncate(node.getVersion(), 32));
            statement.setString(index++, truncate(executorId, 128));
            statement.setString(index++, toJson(config));
            statement.setString(index++, truncate(execution.getProcessInstanceId(), 128));
            statement.setString(index++, truncate(execution.getId(), 128));
            statement.setString(index++, commandType(node.getType()));
            statement.setString(index++, workerTopic(node.getType(), config));
            statement.setString(index++, MESSAGE_NAME);
            statement.setString(index++, correlationKey);
            statement.setString(index++, idempotencyKey);
            statement.setString(index++, encryptedJson(request));
            statement.setObject(index++, null);
            statement.setString(index++, PENDING);
            statement.setInt(index++, 0);
            statement.setInt(index++, maxAttempts);
            statement.setTimestamp(index++, Timestamp.from(now));
            statement.setObject(index++, null);
            statement.setObject(index++, null);
            statement.setString(index++, policy);
            statement.setTimestamp(index++, Timestamp.from(timeoutAt));
            statement.setObject(index++, null);
            statement.setObject(index++, null);
            statement.setObject(index++, null);
            statement.setObject(index++, null);
            statement.setObject(index++, null);
            statement.setTimestamp(index++, Timestamp.from(now));
            statement.setTimestamp(index++, Timestamp.from(now));
            statement.setLong(index++, 0L);
            statement.setLong(index, 0L);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("外部 I/O 命令落库未返回主键");
        return key.longValue();
    }

    private void persistTerminal(Map<String, Object> row, String status, Map<String, Object> output,
                                 String errorCode, String errorMessage, Instant now) {
        jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, response_json = ?, completed_at = ?, "
                        + "next_attempt_at = NULL, lease_owner = NULL, lease_until = NULL, lease_token = NULL, error_code = ?, "
                        + "error_message = ?, update_time = ? WHERE tenant_id = ? AND id = ?",
                status, encryptedJson(output), Timestamp.from(now), truncate(errorCode, 64),
                truncate(errorMessage, 2000), Timestamp.from(now),
                number(row.get("tenant_id")), number(row.get("id")));
    }

    private void resumeTerminal(Map<String, Object> row, Map<String, Object> output) {
        String executionId = text(row.get("execution_id"));
        Object waitValue = flowableRuntimeService.getObject().getVariableLocal(executionId, "hbWaitInstanceId");
        String waitInstanceId = text(row.get("wait_instance_id"));
        if (waitValue == null || !StringUtils.equals(waitInstanceId, String.valueOf(waitValue))) {
            throw new IllegalStateException("命令等待实例与 Flowable execution 不一致");
        }
        FlowResumeCommand command = new FlowResumeCommand();
        command.setTenantId(text(row.get("tenant_id")));
        command.setRunId(text(row.get("run_id")));
        command.setEngineInstanceId(text(row.get("engine_instance_id")));
        command.setExecutionId(executionId);
        command.setWaitInstanceId(waitInstanceId);
        command.setMessageName(text(row.get("message_name")));
        command.setCorrelationKey(text(row.get("correlation_key")));
        command.setPayload(copy(output));
        heartbeatRuntimeService.getObject().resume(command);
    }

    private void moveAmbiguous(Map<String, Object> row, String status, Instant now, String message) {
        jdbcTemplate.update("UPDATE hb_flow_io_command SET status = ?, lease_owner = NULL, lease_until = NULL, "
                        + "lease_token = NULL, error_code = ?, error_message = ?, update_time = ? WHERE tenant_id = ? AND id = ?",
                status, "IO_AMBIGUOUS", message, Timestamp.from(now),
                number(row.get("tenant_id")), number(row.get("id")));
    }

    private Map<String, Object> findByIdempotency(long tenantId, String idempotencyKey) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM hb_flow_io_command WHERE tenant_id = ? AND idempotency_key = ?",
                tenantId, idempotencyKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> load(long tenantId, long commandId, boolean forUpdate) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM hb_flow_io_command WHERE tenant_id = ? AND id = ?" + (forUpdate ? " FOR UPDATE" : ""),
                tenantId, commandId);
        if (rows.isEmpty()) throw new IllegalArgumentException("外部 I/O 命令不存在或不属于当前租户: " + commandId);
        return rows.get(0);
    }

    private void validateDispatchIdentity(Map<String, Object> row, DelegateExecution execution,
                                          FlowNode node, String executorId, long runId) {
        if (number(row.get("run_id")) != runId
                || !StringUtils.equals(text(row.get("node_id")), node.getId())
                || !StringUtils.equals(text(row.get("engine_instance_id")), execution.getProcessInstanceId())
                || !StringUtils.equals(text(row.get("execution_id")), execution.getId())
                || !StringUtils.equals(text(row.get("executor_id")), executorId)) {
            throw new IllegalStateException("外部 I/O 幂等键命中了不同的命令身份");
        }
    }

    private void validateResultIdentity(Map<String, Object> row, DelegateExecution execution,
                                        FlowNode node, long runId) {
        if (number(row.get("run_id")) != runId
                || !StringUtils.equals(text(row.get("node_id")), node.getId())
                || !StringUtils.equals(text(row.get("engine_instance_id")), execution.getProcessInstanceId())
                || !StringUtils.equals(text(row.get("execution_id")), execution.getId())) {
            throw new IllegalStateException("外部 I/O 结果与当前 Flowable execution 不一致");
        }
    }

    private FlowExternalIoCommandView toView(Map<String, Object> row) {
        FlowExternalIoCommandView view = new FlowExternalIoCommandView();
        view.setCommandId(text(row.get("id")));
        view.setRunId(text(row.get("run_id")));
        view.setNodeId(text(row.get("node_id")));
        view.setNodeType(text(row.get("node_type")));
        view.setNodeVersion(text(row.get("node_version")));
        view.setExecutorId(text(row.get("executor_id")));
        view.setWorkerTopic(text(row.get("worker_topic")));
        view.setIdempotencyKey(text(row.get("idempotency_key")));
        view.setStatus(text(row.get("status")));
        view.setAttemptNo((int) number(row.get("attempt_no")));
        view.setMaxAttempts((int) number(row.get("max_attempts")));
        view.setLeaseUntil(instant(row.get("lease_until")));
        view.setLeaseToken(text(row.get("lease_token")));
        view.setLeaseVersion(number(row.get("lease_version")));
        view.setTimeoutAt(instant(row.get("timeout_at")));
        view.setRequest(decryptJson(row.get("request_json")));
        return view;
    }

    private String encryptedJson(Map<String, Object> value) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("format", "AES_GCM_V1");
        envelope.put("ciphertext", secretCryptoService.encryptIfPlain(toJson(copy(value))));
        return toJson(envelope);
    }

    private Map<String, Object> decryptJson(Object databaseValue) {
        if (databaseValue == null) return new LinkedHashMap<>();
        Map<String, Object> envelope = fromJson(databaseText(databaseValue));
        if ("AES_GCM_V1".equals(text(envelope.get("format"))) && envelope.get("ciphertext") != null) {
            return fromJson(secretCryptoService.decryptIfCipher(text(envelope.get("ciphertext"))));
        }
        return envelope;
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("外部 I/O 命令 JSON 解析失败", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("外部 I/O 命令 JSON 序列化失败", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copy(Map<String, Object> value) {
        if (value == null) return new LinkedHashMap<>();
        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {
        });
    }

    private String externalCallPolicy(String nodeType, Map<String, Object> config) {
        String policy = StringUtils.upperCase(text(config.get("externalCallPolicy")), Locale.ROOT);
        if (!Arrays.asList("IDEMPOTENT_RETRY", "QUERY_THEN_RETRY", "MANUAL_ONLY").contains(policy)) {
            if (Boolean.TRUE.equals(config.get("supportsExternalIdempotency"))) return "IDEMPOTENT_RETRY";
            String normalizedType = StringUtils.defaultString(nodeType).toLowerCase(Locale.ROOT);
            String method = StringUtils.upperCase(text(config.get("method")), Locale.ROOT);
            if (normalizedType.contains("mysql.query") || normalizedType.contains("redis.get")
                    || (normalizedType.contains("http") && Arrays.asList("GET", "HEAD", "OPTIONS").contains(method))) {
                return "IDEMPOTENT_RETRY";
            }
            return "MANUAL_ONLY";
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    private int maxAttempts(Map<String, Object> config, String policy) {
        if ("MANUAL_ONLY".equals(policy)) return 1;
        Object retry = config.get("retry");
        Object value = retry instanceof Map ? ((Map<String, Object>) retry).get("maxAttempts") : config.get("maxAttempts");
        try {
            return Math.max(1, Math.min(Integer.parseInt(String.valueOf(value)), 20));
        } catch (Exception ignored) {
            return 3;
        }
    }

    private Duration timeout(Map<String, Object> config) {
        String value = text(config.containsKey("timeout") ? config.get("timeout") : config.get("timeoutMs"));
        if (StringUtils.isBlank(value)) return Duration.ofMinutes(5);
        try {
            Duration duration = value.matches("\\d+") ? Duration.ofMillis(Long.parseLong(value)) : Duration.parse(value);
            if (duration.isNegative() || duration.isZero()) return Duration.ofMinutes(5);
            return duration.compareTo(Duration.ofDays(1)) > 0 ? Duration.ofDays(1) : duration;
        } catch (Exception ignored) {
            return Duration.ofMinutes(5);
        }
    }

    private String workerTopic(String type, Map<String, Object> config) {
        String configured = text(config.get("workerTopic"));
        if (StringUtils.isNotBlank(configured)) return requiredTopic(configured);
        String normalized = StringUtils.defaultString(type).toLowerCase(Locale.ROOT);
        if (normalized.contains("http")) return "flow-io-http";
        if (normalized.contains("mysql")) return "flow-io-db";
        if (normalized.contains("redis")) return "flow-io-redis";
        if (normalized.contains("mq")) return "flow-io-mq";
        return "flow-io-generic";
    }

    private String commandType(String nodeType) {
        String normalized = StringUtils.defaultString(nodeType).toUpperCase(Locale.ROOT);
        if (normalized.contains("HTTP")) return "HTTP";
        if (normalized.contains("MYSQL")) return "MYSQL";
        if (normalized.contains("REDIS")) return "REDIS";
        if (normalized.contains("MQ")) return "MQ";
        return "EXTERNAL_IO";
    }

    private List<String> topics(List<String> values) {
        if (values == null) throw new IllegalArgumentException("workerTopics 不能为空");
        List<String> result = values.stream().filter(StringUtils::isNotBlank).map(this::requiredTopic)
                .distinct().limit(20).collect(Collectors.toList());
        if (result.isEmpty()) throw new IllegalArgumentException("workerTopics 不能为空");
        return result;
    }

    private String completionStatus(String value) {
        String status = StringUtils.upperCase(StringUtils.trimToEmpty(value), Locale.ROOT);
        if ("SUCCESS".equals(status)) status = SUCCEEDED;
        if ("RETRYABLE".equals(status)) status = FAILED_RETRYABLE;
        if ("FAILED".equals(status)) status = FAILED_FINAL;
        if (!Arrays.asList(SUCCEEDED, FAILED_RETRYABLE, FAILED_FINAL, TIMEOUT, CANCELED).contains(status)) {
            throw new IllegalArgumentException("不支持的外部 I/O 完成状态: " + value);
        }
        return status;
    }

    private boolean compatibleCompletion(String current, String requested) {
        if (current.equals(requested)) return true;
        return FAILED_FINAL.equals(current) && FAILED_RETRYABLE.equals(requested);
    }

    private void requireLease(Map<String, Object> row, String workerId, String leaseToken) {
        if (!workerId.equals(text(row.get("lease_owner")))
                || !leaseToken.equals(text(row.get("lease_token")))) {
            throw new IllegalStateException("外部 I/O 命令租约不属于当前 worker");
        }
    }

    private String requiredLeaseToken(String value) {
        if (StringUtils.isBlank(value) || value.length() > 64) {
            throw new IllegalArgumentException("leaseToken 无效");
        }
        return value.trim();
    }

    private void requireActiveLease(Map<String, Object> row) {
        Instant leaseUntil = instant(row.get("lease_until"));
        if (leaseUntil == null || !leaseUntil.isAfter(Instant.now())) {
            throw new IllegalStateException("外部 I/O 命令租约已过期");
        }
    }

    private String requiredWorker(String value) {
        if (StringUtils.isBlank(value)) throw new IllegalArgumentException("workerId 不能为空");
        String workerId = value.trim();
        if (!workerId.matches("[A-Za-z0-9._:@-]{1,128}")) {
            throw new IllegalArgumentException("workerId 格式无效");
        }
        return workerId;
    }

    private String requiredTopic(String value) {
        String topic = StringUtils.trimToEmpty(value);
        if (!topic.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException("workerTopic 格式无效: " + value);
        }
        return topic;
    }

    private int boundedSeconds(int value, int fallback, int min, int max) {
        int normalized = value <= 0 ? fallback : value;
        return Math.max(min, Math.min(normalized, max));
    }

    private Instant leaseUntil(Map<String, Object> row, Instant now, int leaseSeconds) {
        Instant timeoutAt = instant(row.get("timeout_at"));
        if (timeoutAt != null && !timeoutAt.isAfter(now)) {
            throw new IllegalStateException("外部 I/O 命令已超过截止时间");
        }
        Instant requested = now.plusSeconds(leaseSeconds);
        return timeoutAt != null && requested.isAfter(timeoutAt) ? timeoutAt : requested;
    }

    private long positiveLong(String value, String field) {
        try {
            long parsed = Long.parseLong(StringUtils.trimToEmpty(value));
            if (parsed > 0) return parsed;
        } catch (NumberFormatException ignored) {
            // Report a stable field-specific error below.
        }
        throw new IllegalStateException("缺少有效的 " + field);
    }

    private Long nullablePositiveLong(String value) {
        if (StringUtils.isBlank(value)) return null;
        return positiveLong(value, "flowVersionId");
    }

    private long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant) return (Instant) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toInstant();
        if (value instanceof java.util.Date) return ((java.util.Date) value).toInstant();
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).toInstant(ZoneOffset.UTC);
        return Instant.parse(String.valueOf(value));
    }

    private String databaseText(Object value) {
        if (value instanceof byte[]) return new String((byte[]) value, StandardCharsets.UTF_8);
        if (value instanceof Clob) {
            try {
                Clob clob = (Clob) value;
                return clob.getSubString(1, Math.toIntExact(clob.length()));
            } catch (Exception ex) {
                throw new IllegalStateException("外部 I/O 命令内容读取失败", ex);
            }
        }
        return String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item & 0xff));
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("外部 I/O 幂等摘要计算失败", ex);
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void increment(Map<String, Integer> summary, String key) {
        summary.put(key, summary.getOrDefault(key, 0) + 1);
    }
}
