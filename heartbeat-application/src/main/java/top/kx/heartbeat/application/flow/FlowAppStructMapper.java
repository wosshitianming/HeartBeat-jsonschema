package top.kx.heartbeat.application.flow;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.kx.heartbeat.application.flow.dto.FlowDefinitionDTO;
import top.kx.heartbeat.application.flow.param.FlowDefinitionSaveParam;
import top.kx.heartbeat.application.flow.vo.FlowDefinitionVO;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowVersion;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Flow 领域模型 ↔ 应用层 DTO / VO / Param 互转（应用层）
 * <p>
 * 由 MapStruct 自动生成实现，零手写胶水代码。
 * </p>
 *
 * @author heartbeat-team
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FlowAppStructMapper {

    /**
     * 领域模型 → DTO
     */
    FlowDefinitionDTO toDto(FlowDefinition domain);

    /**
     * 领域模型 → VO
     */
    FlowDefinitionVO toVo(FlowDefinition domain);

    /**
     * 领域模型列表 → VO 列表
     */
    List<FlowDefinitionVO> toVoList(List<FlowDefinition> domains);

    /**
     * DTO → 领域模型（应用层向上封口使用）
     */
    FlowDefinition toDomain(FlowDefinitionDTO dto);

    /**
     * 入参 → 领域模型
     */
    @Mapping(target = "variables", ignore = true)
    @Mapping(target = "nodes", ignore = true)
    @Mapping(target = "edges", ignore = true)
    @Mapping(target = "settings", ignore = true)
    @Mapping(target = "activeVersionNo", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    FlowDefinition toDomain(FlowDefinitionSaveParam param);

    /**
     * Instant ↔ LocalDateTime 转换（领域层用 Instant，应用层 DTO 用 LocalDateTime）
     */
    default LocalDateTime mapTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    default Instant mapTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * 流程版本占位（VO 暂未做，预留接口）
     */
    default FlowVersion toDomainVersion(FlowDefinitionDTO dto) {
        return null;
    }
}