package top.kx.heartbeat.infrastructure.flow.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowVersion;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowVersionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowVersionDOWithBLOBs;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Date;

/**
 * Flow 领域模型 ↔ MBG 生成 DO 互转（基础设施层）
 * <p>
 * 由 MapStruct 自动生成实现类，敏感字段（DSL JSON）以 ObjectMapper 序列化/反序列化。
 * </p>
 *
 * @author heartbeat-team
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FlowConvert {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * MBG 生成 DO → 领域模型（FlowDefinition）
     */
    public abstract FlowDefinition toDomain(HbFlowDefinitionDO row);

    /**
     * 领域模型 → MBG 生成 DO（FlowDefinition）
     */
    @Mapping(target = "dslJson", source = ".")
    public abstract HbFlowDefinitionDO toGenDO(FlowDefinition definition);

    /**
     * MBG 生成 DO → 领域模型（FlowVersion）
     */
    @Mapping(target = "flowDsl", source = "dslJson")
    public abstract FlowVersion toDomain(HbFlowVersionDOWithBLOBs row);

    /**
     * MBG 生成 DO → 领域模型（FlowVersion，DSL 字段由调用方补全）
     */
    public FlowVersion toDomainFromBase(HbFlowVersionDO row) {
        if (row == null) {
            return null;
        }
        HbFlowVersionDOWithBLOBs blobs = new HbFlowVersionDOWithBLOBs();
        blobs.setId(row.getId());
        blobs.setTenantId(row.getTenantId());
        blobs.setFlowId(row.getFlowId());
        blobs.setVersionNo(row.getVersionNo());
        blobs.setStatus(row.getStatus());
        blobs.setPublishedBy(row.getPublishedBy());
        blobs.setPublishedAt(row.getPublishedAt());
        blobs.setCreateTime(row.getCreateTime());
        blobs.setUpdateTime(row.getUpdateTime());
        blobs.setCreateBy(row.getCreateBy());
        blobs.setUpdateBy(row.getUpdateBy());
        return toDomain(blobs);
    }

    /**
     * 领域模型 → MBG 生成 DO（FlowVersion）
     */
    @Mapping(target = "dslJson", source = "flowDsl")
    public abstract HbFlowVersionDOWithBLOBs toGenVersionDO(FlowVersion version);

    /**
     * 工厂：解析 DSL JSON 后再生成 FlowDefinition（用 ObjectFactory 让 MapStruct 保留工厂）
     */
    @ObjectFactory
    protected FlowDefinition createDefinition(HbFlowDefinitionDO row) {
        return readDefinition(row == null ? null : row.getDslJson());
    }

    protected FlowDefinition readDefinition(String json) {
        if (StringUtils.isBlank(json)) {
            return new FlowDefinition();
        }
        try {
            return objectMapper.readValue(json, FlowDefinition.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Flow DSL 解析失败", ex);
        }
    }

    protected String writeDefinition(FlowDefinition definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Flow DSL 序列化失败", ex);
        }
    }

    /**
     * Instant ↔ LocalDateTime 转换（领域层使用 Instant，DB 层使用 LocalDateTime）
     */
    public Date mapTime(Instant value) {
        return value == null ? null : Date.from(value);
    }

    protected Instant mapTime(Date value) {
        return value == null ? null : value.toInstant();
    }

    /**
     * Long id ↔ String id（DB BIGINT ↔ 领域层 String）
     */
    protected String mapId(Long value) {
        return value == null ? null : value.toString();
    }

    protected Long mapId(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
