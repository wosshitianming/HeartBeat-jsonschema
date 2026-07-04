package top.kx.heartbeat.infrastructure.flow;

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
public abstract class FlowRunStructMapper {

    @Resource
    private ObjectMapper objectMapper;

    public abstract FlowRun toDomain(HbFlowRunDOWithBLOBs entity);

    public FlowRun toDomainFromBase(HbFlowRunDO row) {
        if (row == null) {
            return null;
        }
        HbFlowRunDOWithBLOBs blobs = new HbFlowRunDOWithBLOBs();
        blobs.setId(row.getId());
        blobs.setTenantId(row.getTenantId());
        blobs.setFlowId(row.getFlowId());
        blobs.setVersionNo(row.getVersionNo());
        blobs.setTriggerType(row.getTriggerType());
        blobs.setStatus(row.getStatus());
        blobs.setStartedAt(row.getStartedAt());
        blobs.setFinishedAt(row.getFinishedAt());
        blobs.setElapsedMs(row.getElapsedMs());
        blobs.setCreateTime(row.getCreateTime());
        blobs.setUpdateTime(row.getUpdateTime());
        blobs.setCreateBy(row.getCreateBy());
        blobs.setUpdateBy(row.getUpdateBy());
        return toDomain(blobs);
    }

    public FlowRunEvent toEventDomainFromBase(HbFlowRunEventDO row) {
        if (row == null) {
            return null;
        }
        HbFlowRunEventDOWithBLOBs blobs = new HbFlowRunEventDOWithBLOBs();
        blobs.setId(row.getId());
        blobs.setTenantId(row.getTenantId());
        blobs.setRunId(row.getRunId());
        blobs.setNodeId(row.getNodeId());
        blobs.setNodeType(row.getNodeType());
        blobs.setEventType(row.getEventType());
        blobs.setElapsedMs(row.getElapsedMs());
        blobs.setCreateTime(row.getCreateTime());
        blobs.setUpdateTime(row.getUpdateTime());
        blobs.setCreateBy(row.getCreateBy());
        blobs.setUpdateBy(row.getUpdateBy());
        return toDomain(blobs);
    }

    @Mapping(target = "input", source = "inputJson")
    @Mapping(target = "output", source = "outputJson")
    public abstract FlowRunEvent toDomain(HbFlowRunEventDOWithBLOBs entity);

    public abstract HbFlowRunDOWithBLOBs toGenDO(FlowRun run);

    @Mapping(target = "inputJson", source = "input")
    @Mapping(target = "outputJson", source = "output")
    public abstract HbFlowRunEventDOWithBLOBs toGenEventDO(FlowRunEvent event);

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapJson(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("流程运行 JSON 解析失败", ex);
        }
    }

    protected String mapJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (JsonProcessingException ex) {
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
