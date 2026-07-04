package top.kx.heartbeat.infrastructure.flow.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;
import top.kx.heartbeat.infrastructure.flow.convert.FlowRunConvert;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunEventDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunEventDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowRunDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowRunEventDOMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class FlowRunRepositoryImpl implements FlowRunRepository {

    @Autowired
    private HbFlowRunDOMapper runDOMapper;

    @Autowired
    private HbFlowRunEventDOMapper eventDOMapper;

    @Autowired
    private FlowRunConvert convert;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public FlowRun saveRun(FlowRun run) {
        HbFlowRunDOWithBLOBs record = convert.toGenDO(run);
        runDOMapper.insertSelective(record);
        updateRuntimeFields(run);
        return findRun(run.getId()).orElse(run);
    }

    @Override
    public FlowRunEvent saveEvent(FlowRunEvent event) {
        HbFlowRunEventDOWithBLOBs record = convert.toGenEventDO(event);
        eventDOMapper.insertSelective(record);
        return event;
    }

    @Override
    public Optional<FlowRun> findRun(String runId) {
        HbFlowRunDOWithBLOBs record = runDOMapper.selectByPrimaryKey(parseLong(runId));
        return Optional.ofNullable(record).map(convert::toDomain).map(this::enrichRuntimeFields);
    }

    @Override
    public List<FlowRun> findRunsByFlowId(String flowId) {
        HbFlowRunDOExample example = new HbFlowRunDOExample();
        example.createCriteria().andFlowIdEqualTo(parseLong(flowId));
        example.setOrderByClause("started_at DESC");
        return runDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(convert::toDomain)
                .map(this::enrichRuntimeFields)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlowRunEvent> findEvents(String runId) {
        HbFlowRunEventDOExample example = new HbFlowRunEventDOExample();
        HbFlowRunEventDOExample.Criteria criteria = example.createCriteria();
        criteria.andRunIdEqualTo(parseLong(runId));
        example.setOrderByClause("created_at ASC");
        return eventDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(convert::toDomain)
                .collect(Collectors.toList());
    }

    private static Long parseLong(String s) {
        if (s == null || s.trim().isEmpty()) {
            return -1L;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    /**
     * 更新 MBG 尚未覆盖的运行时扩展字段。
     *
     * @param run 流程运行记录
     */
    private void updateRuntimeFields(FlowRun run) {
        jdbcTemplate.update(
                "UPDATE hb_flow_run SET run_no = ?, engine = ?, engine_instance_id = ?, process_definition_id = ?, flow_version_id = ?, trigger_id = ?, trigger_key = ?, idempotency_key = ?, idempotency_scope = ?, business_key = ?, correlation_key = ?, parent_run_id = ?, root_run_id = ?, retry_from_run_id = ?, retry_no = ?, retry_reason = ?, tenant_id = ? WHERE id = ?",
                run.getRunNo(),
                run.getEngine(),
                run.getEngineInstanceId(),
                run.getProcessDefinitionId(),
                parseNullableLong(run.getFlowVersionId()),
                parseNullableLong(run.getTriggerId()),
                run.getTriggerKey(),
                run.getIdempotencyKey(),
                run.getIdempotencyScope(),
                run.getBusinessKey(),
                run.getCorrelationKey(),
                parseNullableLong(run.getParentRunId()),
                parseNullableLong(run.getRootRunId()),
                parseNullableLong(run.getRetryFromRunId()),
                run.getRetryNo() == null ? 0 : run.getRetryNo(),
                run.getRetryReason(),
                parseNullableLong(run.getTenantId()) == null ? 1L : parseNullableLong(run.getTenantId()),
                parseLong(run.getId())
        );
    }

    /**
     * 解析可空 Long 值。
     *
     * @param s 字符串值
     * @return 可空 Long 值
     */
    private Long parseNullableLong(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 读取 MBG 尚未覆盖的运行时扩展字段。
     *
     * @param run 流程运行记录
     * @return 补齐扩展字段后的运行记录
     */
    private FlowRun enrichRuntimeFields(FlowRun run) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT run_no, engine, engine_instance_id, process_definition_id, flow_version_id, trigger_id, trigger_key, idempotency_key, idempotency_scope, business_key, correlation_key, parent_run_id, root_run_id, retry_from_run_id, retry_no, retry_reason, tenant_id FROM hb_flow_run WHERE id = ?",
                parseLong(run.getId())
        );
        if (rows.isEmpty()) {
            return run;
        }
        Map<String, Object> row = rows.get(0);
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
        return run;
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
