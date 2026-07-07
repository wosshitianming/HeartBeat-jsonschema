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
    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract HbFlowDefinitionDO toGenDO(FlowDefinition definition);

    /**
     * MBG 生成 DO → 领域模型（FlowVersion）
     */
    @Mapping(target = "flowDsl", source = "dslJson")
    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
    public abstract FlowVersion toDomain(HbFlowVersionDOWithBLOBs row);

    /**
     * MBG 生成 DO → 领域模型（FlowVersion，DSL 字段由调用方补全）
     */
    public FlowVersion toDomainFromBase(HbFlowVersionDO row) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row == null) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 创建当前流程需要的临时对象，承载后续处理数据。
        HbFlowVersionDOWithBLOBs blobs = new HbFlowVersionDOWithBLOBs();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setId(row.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setTenantId(row.getTenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setFlowId(row.getFlowId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setVersionNo(row.getVersionNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setStatus(row.getStatus());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setPublishedBy(row.getPublishedBy());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        blobs.setPublishedAt(row.getPublishedAt());
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
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isBlank(json)) {
            // 返回已经完成封装的业务结果。
            return new FlowDefinition();
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readValue(json, FlowDefinition.class);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("Flow DSL 解析失败", ex);
        }
    }

    protected String writeDefinition(FlowDefinition definition) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
