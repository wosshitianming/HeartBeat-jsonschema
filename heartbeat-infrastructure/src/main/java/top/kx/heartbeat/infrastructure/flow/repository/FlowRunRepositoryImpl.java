package top.kx.heartbeat.infrastructure.flow.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.model.FlowRunQuery;
import top.kx.heartbeat.domain.flow.model.FlowRunStatistics;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;
import top.kx.heartbeat.infrastructure.flow.convert.FlowRunConvert;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.FlowRunRuntimeMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowDefinitionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowRunDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowRunEventDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class FlowRunRepositoryImpl implements FlowRunRepository {

    @Resource
    private HbFlowRunDOMapper runDOMapper;

    @Resource
    private HbFlowDefinitionDOMapper definitionDOMapper;

    @Resource
    private HbFlowRunEventDOMapper eventDOMapper;

    @Resource
    private FlowRunConvert convert;

    @Resource
    private FlowRunRuntimeMapper runtimeMapper;

    @Resource
    private ObjectMapper objectMapper;

    private static Long parseLong(String s) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (StringUtils.isBlank(s)) {
            // 返回已经完成封装的业务结果。
            return -1L;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return Long.valueOf(s.trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return -1L;
        }
    }

    @Override
    @Transactional
    public FlowRun saveRun(FlowRun run) {
        long tenantId = tenantId();
        Long flowId = parseLong(run.getFlowId());
        if (!ownsFlow(flowId, tenantId)) {
            throw new IllegalStateException("流程不存在或不属于当前租户: " + run.getFlowId());
        }
        HbFlowRunDOWithBLOBs record = convert.toGenDO(run);
        record.setTenantId(tenantId);
        HbFlowRunDOWithBLOBs existing = record.getId() == null ? null : selectRun(record.getId(), tenantId);
        if (existing == null) {
            runDOMapper.insertSelective(record);
        } else {
            runDOMapper.updateByExampleSelective(record, runById(record.getId(), tenantId));
        }
        if (record.getId() != null) {
            run.setId(String.valueOf(record.getId()));
        }
        run.setTenantId(String.valueOf(tenantId));
        updateRuntimeFields(run, tenantId);
        return findRun(run.getId()).orElse(run);
    }

    @Override
    public Optional<FlowRun> findRun(String runId) {
        long tenantId = tenantId();
        HbFlowRunDOWithBLOBs record = selectRun(parseLong(runId), tenantId);
        return Optional.ofNullable(record)
                .map(convert::toDomain)
                .map(run -> enrichRuntimeFields(run, tenantId));
    }

    @Override
    @Transactional
    public FlowRunEvent saveEvent(FlowRunEvent event) {
        long tenantId = tenantId();
        Long runId = parseLong(event.getRunId());
        if (selectRun(runId, tenantId) == null) {
            throw new IllegalStateException("流程运行不存在或不属于当前租户: " + event.getRunId());
        }
        event.setEventSeq(nextEventSequence(runId, tenantId));
        HbFlowRunEventDOWithBLOBs record = convert.toGenEventDO(event);
        record.setTenantId(tenantId);
        eventDOMapper.insertSelective(record);
        if (record.getId() != null) {
            event.setId(String.valueOf(record.getId()));
        }
        event.setTenantId(String.valueOf(tenantId));
        updateEventRuntimeFields(event, tenantId);
        return event;
    }

    @Override
    @Transactional
    public Optional<FlowRun> findRunForUpdate(String runId) {
        long tenantId = tenantId();
        Long id = parseLong(runId);
        if (id == null || id < 0) return Optional.empty();
        return Optional.ofNullable(runtimeMapper.selectRunForUpdate(id, tenantId)).map(this::mapLockedRun);
    }

    @Override
    public Optional<FlowRun> findRunByEngineInstanceId(String engineInstanceId) {
        if (StringUtils.isBlank(engineInstanceId)) return Optional.empty();
        Long runId = runtimeMapper.selectRunIdByEngineInstanceId(tenantId(), engineInstanceId.trim());
        return runId == null ? Optional.empty() : findRun(String.valueOf(runId));
    }

    @Override
    public List<FlowRun> findRunsByFlowId(String flowId) {
        long tenantId = tenantId();
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        HbFlowRunDOExample example = new HbFlowRunDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andFlowIdEqualTo(parseLong(flowId));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("started_at DESC");
        // 返回已经完成封装的业务结果。
        List<FlowRun> runs = runDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(convert::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
        return enrichRuntimeFields(runs, tenantId);
    }

    @Override
    public Page<FlowRun> pageByQuery(FlowRunQuery query) {
        FlowRunQuery safe = query == null ? new FlowRunQuery() : query;
        long tenantId = tenantId();
        HbFlowRunDOExample example = new HbFlowRunDOExample();
        HbFlowRunDOExample.Criteria criteria = example.createCriteria();
        criteria.andTenantIdEqualTo(tenantId);
        if (StringUtils.isNotBlank(safe.getFlowId())) {
            criteria.andFlowIdEqualTo(parseLong(safe.getFlowId()));
        }
        if (safe.getStatuses() != null && !safe.getStatuses().isEmpty()) criteria.andStatusIn(safe.getStatuses());
        if (safe.getTriggerTypes() != null && !safe.getTriggerTypes().isEmpty())
            criteria.andTriggerTypeIn(safe.getTriggerTypes());
        if (safe.getStartedAfter() != null)
            criteria.andStartedAtGreaterThanOrEqualTo(java.util.Date.from(safe.getStartedAfter()));
        if (safe.getStartedBefore() != null)
            criteria.andStartedAtLessThanOrEqualTo(java.util.Date.from(safe.getStartedBefore()));
        example.setOrderByClause(resolveOrderByColumn(safe.getOrderByColumn()) + " " + resolveOrderDirection(safe.getOrderByDirection()));
        PageHelper.startPage(safe.getPageNum(), safe.getPageSize());
        List<HbFlowRunDO> rows = runDOMapper.selectByExample(example);
        long total = rows instanceof com.github.pagehelper.Page
                ? ((com.github.pagehelper.Page<?>) rows).getTotal() : rows.size();
        List<FlowRun> records = rows.stream().map(convert::toDomainFromBase).collect(Collectors.toList());
        return new Page<>(enrichRuntimeFields(records, tenantId), total, safe.getPageNum(), safe.getPageSize());
    }

    @Override
    public FlowRunStatistics summarize(String flowId, Instant startedAfter, Instant startedBefore) {
        Map<String, Object> row = runtimeMapper.summarize(
                tenantId(), StringUtils.isBlank(flowId) ? null : parseLong(flowId),
                startedAfter == null ? null : Date.from(startedAfter),
                startedBefore == null ? null : Date.from(startedBefore));
        FlowRunStatistics stats = new FlowRunStatistics();
        stats.setTotalRuns(longValue(column(row, "total_runs")));
        stats.setRunningRuns(longValue(column(row, "running_runs")));
        stats.setWaitingRuns(longValue(column(row, "waiting_runs")));
        stats.setSuccessRuns(longValue(column(row, "success_runs")));
        stats.setFailedRuns(longValue(column(row, "failed_runs")));
        stats.setCanceledRuns(longValue(column(row, "canceled_runs")));
        stats.setAverageDurationMs(longValue(column(row, "average_duration_ms")));
        return stats;
    }

    @Override
    public List<FlowRunEvent> findEvents(String runId) {
        long tenantId = tenantId();
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        HbFlowRunEventDOExample example = new HbFlowRunEventDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        HbFlowRunEventDOExample.Criteria criteria = example.createCriteria();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        criteria.andTenantIdEqualTo(tenantId).andRunIdEqualTo(parseLong(runId));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("event_seq ASC, create_time ASC");
        // 返回已经完成封装的业务结果。
        List<FlowRunEvent> events = eventDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(convert::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
        return enrichEventRuntimeFields(events, tenantId);
    }

    /**
     * 更新 MBG 尚未覆盖的运行时扩展字段。
     *
     * @param run 流程运行记录
     */
    private void updateRuntimeFields(FlowRun run, long tenantId) {
        Map<String, Object> values = new HashMap<>();
        values.put("runNo", run.getRunNo());
        values.put("engine", run.getEngine());
        values.put("engineInstanceId", run.getEngineInstanceId());
        values.put("processDefinitionId", run.getProcessDefinitionId());
        values.put("flowVersionId", parseNullableLong(run.getFlowVersionId()));
        values.put("triggerId", parseNullableLong(run.getTriggerId()));
        values.put("triggerKey", run.getTriggerKey());
        values.put("idempotencyKey", run.getIdempotencyKey());
        values.put("idempotencyScope", run.getIdempotencyScope());
        values.put("businessKey", run.getBusinessKey());
        values.put("correlationKey", run.getCorrelationKey());
        values.put("parentRunId", parseNullableLong(run.getParentRunId()));
        values.put("rootRunId", parseNullableLong(run.getRootRunId()));
        values.put("retryFromRunId", parseNullableLong(run.getRetryFromRunId()));
        values.put("retryNo", run.getRetryNo() == null ? 0 : run.getRetryNo());
        values.put("retryReason", run.getRetryReason());
        int updated = runtimeMapper.updateRunRuntime(parseLong(run.getId()), tenantId, values);
        if (updated != 1) {
            throw new IllegalStateException("流程运行扩展字段更新失败: " + run.getId());
        }
    }

    private long nextEventSequence(Long runId, long tenantId) {
        Long current = runtimeMapper.selectLastEventSequenceForUpdate(runId, tenantId);
        long next = (current == null ? 0L : current) + 1L;
        if (runtimeMapper.updateLastEventSequence(runId, tenantId, next) != 1) {
            throw new IllegalStateException("流程运行事件序号分配失败: " + runId);
        }
        return next;
    }

    private void updateEventRuntimeFields(FlowRunEvent event, long tenantId) {
        Map<String, Object> values = new HashMap<>();
        values.put("eventSeq", event.getEventSeq());
        values.put("engineActivityId", event.getEngineActivityId());
        values.put("executionId", event.getExecutionId());
        values.put("taskId", event.getTaskId());
        values.put("sourceNodeId", event.getSourceNodeId());
        values.put("targetNodeId", event.getTargetNodeId());
        values.put("edgeId", event.getEdgeId());
        values.put("tokenId", event.getTokenId());
        values.put("attemptNo", event.getAttemptNo() == null ? 1 : event.getAttemptNo());
        values.put("selectedPorts", toJson(event.getSelectedPorts()));
        values.put("inputPayloadRef", parseNullableLong(event.getInputPayloadRef()));
        values.put("outputPayloadRef", parseNullableLong(event.getOutputPayloadRef()));
        values.put("eventSummary", toJson(event.getEventSummary()));
        values.put("errorCode", event.getErrorCode());
        int updated = runtimeMapper.updateEventRuntime(parseLong(event.getId()), tenantId, values);
        if (updated != 1) {
            throw new IllegalStateException("流程运行事件扩展字段更新失败: " + event.getId());
        }
    }

    /**
     * 解析可空 Long 值。
     *
     * @param s 字符串值
     * @return 可空 Long 值
     */
    private Long parseNullableLong(String s) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (StringUtils.isBlank(s)) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return Long.valueOf(s.trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return null;
        }
    }

    /**
     * 读取 MBG 尚未覆盖的运行时扩展字段。
     *
     * @param run 流程运行记录
     * @return 补齐扩展字段后的运行记录
     */
    private FlowRun enrichRuntimeFields(FlowRun run, long tenantId) {
        Map<String, Object> row = runtimeMapper.selectRunRuntime(parseLong(run.getId()), tenantId);
        if (row == null || row.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return run;
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setRunNo(toString(row.get("run_no")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setEngine(toString(row.get("engine")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setEngineInstanceId(toString(row.get("engine_instance_id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setProcessDefinitionId(toString(row.get("process_definition_id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setFlowVersionId(toString(row.get("flow_version_id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setTriggerId(toString(row.get("trigger_id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setTriggerKey(toString(row.get("trigger_key")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setIdempotencyKey(toString(row.get("idempotency_key")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setIdempotencyScope(toString(row.get("idempotency_scope")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setBusinessKey(toString(row.get("business_key")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setCorrelationKey(toString(row.get("correlation_key")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setParentRunId(toString(row.get("parent_run_id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setRootRunId(toString(row.get("root_run_id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setRetryFromRunId(toString(row.get("retry_from_run_id")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setRetryNo(row.get("retry_no") == null ? null : Integer.valueOf(String.valueOf(row.get("retry_no"))));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setRetryReason(toString(row.get("retry_reason")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        run.setTenantId(toString(row.get("tenant_id")));
        // 返回已经完成封装的业务结果。
        return run;
    }

    private List<FlowRun> enrichRuntimeFields(List<FlowRun> runs, long tenantId) {
        if (runs == null || runs.isEmpty()) return runs == null ? Collections.emptyList() : runs;
        List<Long> ids = runs.stream().map(FlowRun::getId).map(FlowRunRepositoryImpl::parseLong)
                .filter(id -> id != null && id >= 0).collect(Collectors.toList());
        if (ids.isEmpty()) return runs;
        List<Map<String, Object>> rows = runtimeMapper.selectRunRuntimes(tenantId, ids);
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> row : rows) byId.put(toString(row.get("id")), row);
        for (FlowRun run : runs) {
            Map<String, Object> row = byId.get(run.getId());
            if (row != null) applyRuntimeFields(run, row);
        }
        return runs;
    }

    private void applyRuntimeFields(FlowRun run, Map<String, Object> row) {
        run.setRunNo(toString(row.get("run_no")));
        run.setEngine(toString(row.get("engine")));
        run.setEngineInstanceId(toString(row.get("engine_instance_id")));
        run.setProcessDefinitionId(toString(row.get("process_definition_id")));
        run.setFlowVersionId(toString(row.get("flow_version_id")));
        run.setTriggerId(toString(row.get("trigger_id")));
        run.setTriggerKey(toString(row.get("trigger_key")));
        run.setIdempotencyKey(toString(row.get("idempotency_key")));
        run.setIdempotencyScope(toString(row.get("idempotency_scope")));
        run.setBusinessKey(toString(row.get("business_key")));
        run.setCorrelationKey(toString(row.get("correlation_key")));
        run.setParentRunId(toString(row.get("parent_run_id")));
        run.setRootRunId(toString(row.get("root_run_id")));
        run.setRetryFromRunId(toString(row.get("retry_from_run_id")));
        run.setRetryNo(row.get("retry_no") == null ? null : Integer.valueOf(String.valueOf(row.get("retry_no"))));
        run.setRetryReason(toString(row.get("retry_reason")));
        run.setTenantId(toString(row.get("tenant_id")));
    }

    private List<FlowRunEvent> enrichEventRuntimeFields(List<FlowRunEvent> events, long tenantId) {
        if (events == null || events.isEmpty()) return events == null ? Collections.emptyList() : events;
        List<Long> ids = events.stream().map(FlowRunEvent::getId).map(FlowRunRepositoryImpl::parseLong)
                .filter(id -> id != null && id >= 0).collect(Collectors.toList());
        if (ids.isEmpty()) return events;
        List<Map<String, Object>> rows = runtimeMapper.selectEventRuntimes(tenantId, ids);
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> row : rows) byId.put(toString(row.get("id")), row);
        for (FlowRunEvent event : events) {
            Map<String, Object> row = byId.get(event.getId());
            if (row == null) continue;
            event.setEventSeq(row.get("event_seq") == null ? null : longValue(row.get("event_seq")));
            event.setEngineActivityId(toString(row.get("engine_activity_id")));
            event.setExecutionId(toString(row.get("execution_id")));
            event.setTaskId(toString(row.get("task_id")));
            event.setSourceNodeId(toString(row.get("source_node_id")));
            event.setTargetNodeId(toString(row.get("target_node_id")));
            event.setEdgeId(toString(row.get("edge_id")));
            event.setTokenId(toString(row.get("token_id")));
            event.setAttemptNo(row.get("attempt_no") == null ? null : Integer.valueOf(String.valueOf(row.get("attempt_no"))));
            event.setSelectedPorts(readJsonMap(row.get("selected_ports")));
            event.setInputPayloadRef(toString(row.get("input_payload_ref")));
            event.setOutputPayloadRef(toString(row.get("output_payload_ref")));
            event.setEventSummary(readJsonMap(row.get("event_summary")));
            event.setErrorCode(toString(row.get("error_code")));
        }
        return events;
    }

    private String resolveOrderByColumn(String value) {
        if (value == null) return "started_at";
        switch (value.trim()) {
            case "id":
                return "id";
            case "status":
                return "status";
            case "versionNo":
            case "version_no":
                return "version_no";
            case "triggerType":
            case "trigger_type":
                return "trigger_type";
            case "finishedAt":
            case "finished_at":
                return "finished_at";
            case "elapsedMs":
            case "elapsed_ms":
                return "elapsed_ms";
            default:
                return "started_at";
        }
    }

    private String resolveOrderDirection(String value) {
        return "ASC".equalsIgnoreCase(value) ? "ASC" : "DESC";
    }

    private long longValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private Object column(Map<String, Object> row, String name) {
        if (row.containsKey(name)) return row.get(name);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonMap(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) return new HashMap<>();
        try {
            return objectMapper.readValue(String.valueOf(value), Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("流程运行事件 JSON 解析失败", ex);
        }
    }

    private FlowRun mapLockedRun(Map<String, Object> row) {
        FlowRun run = new FlowRun();
        run.setId(toString(column(row, "id")));
        run.setTenantId(toString(column(row, "tenant_id")));
        run.setFlowId(toString(column(row, "flow_id")));
        run.setRunNo(toString(column(row, "run_no")));
        run.setEngine(toString(column(row, "engine")));
        run.setEngineInstanceId(toString(column(row, "engine_instance_id")));
        run.setProcessDefinitionId(toString(column(row, "process_definition_id")));
        run.setFlowVersionId(toString(column(row, "flow_version_id")));
        run.setTriggerId(toString(column(row, "trigger_id")));
        run.setTriggerKey(toString(column(row, "trigger_key")));
        run.setIdempotencyKey(toString(column(row, "idempotency_key")));
        run.setIdempotencyScope(toString(column(row, "idempotency_scope")));
        run.setBusinessKey(toString(column(row, "business_key")));
        run.setCorrelationKey(toString(column(row, "correlation_key")));
        run.setParentRunId(toString(column(row, "parent_run_id")));
        run.setRootRunId(toString(column(row, "root_run_id")));
        run.setRetryFromRunId(toString(column(row, "retry_from_run_id")));
        Object retryNo = column(row, "retry_no");
        run.setRetryNo(retryNo == null ? null : ((Number) retryNo).intValue());
        run.setRetryReason(toString(column(row, "retry_reason")));
        run.setVersionNo(((Number) column(row, "version_no")).intValue());
        run.setTriggerType(toString(column(row, "trigger_type")));
        run.setStatus(toString(column(row, "status")));
        run.setInputSummary(readJsonMap(column(row, "input_summary")));
        run.setOutputSummary(readJsonMap(column(row, "output_summary")));
        run.setErrorMessage(toString(column(row, "error_message")));
        run.setStartedAt(toInstant(column(row, "started_at")));
        run.setFinishedAt(toInstant(column(row, "finished_at")));
        Object elapsedMs = column(row, "elapsed_ms");
        run.setElapsedMs(elapsedMs == null ? null : ((Number) elapsedMs).longValue());
        return run;
    }

    private Instant toInstant(Object value) {
        return value instanceof Date ? ((Date) value).toInstant() : null;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyMap() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("流程运行事件 JSON 序列化失败", ex);
        }
    }

    private HbFlowRunDOWithBLOBs selectRun(Long id, long tenantId) {
        if (id == null || id < 0) {
            return null;
        }
        List<HbFlowRunDOWithBLOBs> rows = runDOMapper.selectByExampleWithBLOBs(runById(id, tenantId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private HbFlowRunDOExample runById(Long id, long tenantId) {
        HbFlowRunDOExample example = new HbFlowRunDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andIdEqualTo(id);
        return example;
    }

    private boolean ownsFlow(Long flowId, long tenantId) {
        if (flowId == null || flowId < 0) {
            return false;
        }
        HbFlowDefinitionDOExample example = new HbFlowDefinitionDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andIdEqualTo(flowId);
        return definitionDOMapper.countByExample(example) > 0;
    }

    private long tenantId() {
        return TenantContext.getRequiredTenantId();
    }

    /**
     * 安全转换字符串。
     *
     * @param value 原始值
     * @return 字符串值
     */
    private String toString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
