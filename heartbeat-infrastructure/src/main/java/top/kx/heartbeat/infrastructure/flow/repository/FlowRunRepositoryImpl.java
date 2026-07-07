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

    private static Long parseLong(String s) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (s == null || s.trim().isEmpty()) {
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
    public List<FlowRun> findRunsByFlowId(String flowId) {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        HbFlowRunDOExample example = new HbFlowRunDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andFlowIdEqualTo(parseLong(flowId));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("started_at DESC");
        // 返回已经完成封装的业务结果。
        return runDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(convert::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::enrichRuntimeFields)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    @Override
    public List<FlowRunEvent> findEvents(String runId) {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        HbFlowRunEventDOExample example = new HbFlowRunEventDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        HbFlowRunEventDOExample.Criteria criteria = example.createCriteria();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        criteria.andRunIdEqualTo(parseLong(runId));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("created_at ASC");
        // 返回已经完成封装的业务结果。
        return eventDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(convert::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 更新 MBG 尚未覆盖的运行时扩展字段。
     *
     * @param run 流程运行记录
     */
    private void updateRuntimeFields(FlowRun run) {
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        jdbcTemplate.update(
                // 计算当前分支的中间结果，供后续判断或组装使用。
                "UPDATE hb_flow_run SET run_no = ?, engine = ?, engine_instance_id = ?, process_definition_id = ?, flow_version_id = ?, trigger_id = ?, trigger_key = ?, idempotency_key = ?, idempotency_scope = ?, business_key = ?, correlation_key = ?, parent_run_id = ?, root_run_id = ?, retry_from_run_id = ?, retry_no = ?, retry_reason = ?, tenant_id = ? WHERE id = ?",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getRunNo(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getEngine(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getEngineInstanceId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getProcessDefinitionId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseNullableLong(run.getFlowVersionId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseNullableLong(run.getTriggerId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getTriggerKey(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getIdempotencyKey(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getIdempotencyScope(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getBusinessKey(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getCorrelationKey(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseNullableLong(run.getParentRunId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseNullableLong(run.getRootRunId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseNullableLong(run.getRetryFromRunId()),
                // 计算当前分支的中间结果，供后续判断或组装使用。
                run.getRetryNo() == null ? 0 : run.getRetryNo(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                run.getRetryReason(),
                // 计算当前步骤所需的中间值，供后续业务判断使用。
                parseNullableLong(run.getTenantId()) == null ? 1L : parseNullableLong(run.getTenantId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (s == null || s.trim().isEmpty()) {
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
    private FlowRun enrichRuntimeFields(FlowRun run) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                // 计算当前分支的中间结果，供后续判断或组装使用。
                "SELECT run_no, engine, engine_instance_id, process_definition_id, flow_version_id, trigger_id, trigger_key, idempotency_key, idempotency_scope, business_key, correlation_key, parent_run_id, root_run_id, retry_from_run_id, retry_no, retry_reason, tenant_id FROM hb_flow_run WHERE id = ?",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseLong(run.getId())
        );
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (rows.isEmpty()) {
            // 返回已经完成封装的业务结果。
            return run;
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> row = rows.get(0);
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
