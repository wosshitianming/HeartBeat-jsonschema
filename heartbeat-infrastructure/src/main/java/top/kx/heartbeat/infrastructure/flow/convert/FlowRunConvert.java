package top.kx.heartbeat.infrastructure.flow.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunDO;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunEventDO;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowRunEventDOWithBLOBs;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FlowRunConvert {

    @Resource
    private ObjectMapper objectMapper;

    public abstract FlowRun toDomain(HbFlowRunDOWithBLOBs entity);

    public FlowRun toDomainFromBase(HbFlowRunDO row) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row == null) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 创建当前流程需要的临时对象，承载后续处理数据。
        HbFlowRunDOWithBLOBs blobs = new HbFlowRunDOWithBLOBs();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setId(row.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setTenantId(row.getTenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setFlowId(row.getFlowId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setVersionNo(row.getVersionNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setTriggerType(row.getTriggerType());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setStatus(row.getStatus());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setStartedAt(row.getStartedAt());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setFinishedAt(row.getFinishedAt());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setElapsedMs(row.getElapsedMs());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setCreateTime(row.getCreateTime());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setUpdateTime(row.getUpdateTime());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setCreateBy(row.getCreateBy());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setUpdateBy(row.getUpdateBy());
        // 返回已经完成封装的业务结果。
        return toDomain(blobs);
    }

    public FlowRunEvent toEventDomainFromBase(HbFlowRunEventDO row) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row == null) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 组装工作流事件状态，保证可靠事件可以被后续消费。
        HbFlowRunEventDOWithBLOBs blobs = new HbFlowRunEventDOWithBLOBs();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setId(row.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setTenantId(row.getTenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setRunId(row.getRunId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setNodeId(row.getNodeId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setNodeType(row.getNodeType());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setEventType(row.getEventType());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setElapsedMs(row.getElapsedMs());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setCreateTime(row.getCreateTime());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setUpdateTime(row.getUpdateTime());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setCreateBy(row.getCreateBy());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setUpdateBy(row.getUpdateBy());
        // 返回已经完成封装的业务结果。
        return toDomain(blobs);
    }

    @Mapping(target = "input", source = "inputJson")
    @Mapping(target = "output", source = "outputJson")
    public abstract FlowRunEvent toDomain(HbFlowRunEventDOWithBLOBs entity);

    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract HbFlowRunDOWithBLOBs toGenDO(FlowRun run);

    @Mapping(target = "inputJson", source = "input")
    @Mapping(target = "outputJson", source = "output")
    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract HbFlowRunEventDOWithBLOBs toGenEventDO(FlowRunEvent event);

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapJson(String json) {
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isBlank(json)) {
            // 返回已经完成封装的业务结果。
            return new LinkedHashMap<>();
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("流程运行 JSON 解析失败", ex);
        }
    }

    protected String mapJson(Map<String, Object> value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("流程运行 JSON 序列化失败", ex);
        }
    }

    protected Date mapTime(Instant value) {
        return value == null ? null : Date.from(value);
    }

    protected Instant mapTime(Date value) {
        return value == null ? null : value.toInstant();
    }
}
