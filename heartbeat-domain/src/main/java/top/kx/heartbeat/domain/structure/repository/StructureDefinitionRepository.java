package top.kx.heartbeat.domain.structure.repository;

import top.kx.heartbeat.domain.structure.model.StructureDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 结构化定义领域仓储接口
 * <p>
 * application 层通过该接口与基础设施层解耦，由 StructureDefinitionRepositoryImpl 在 infrastructure 层实现。
 * </p>
 *
 * @author heartbeat-team
 */
public interface StructureDefinitionRepository {

    /**
     * 保存或更新一条结构化定义
     *
     * @param definition 领域模型
     * @return 持久化后的领域模型（含 ID/版本号等）
     */
    StructureDefinition save(StructureDefinition definition);

    /**
     * 按主键查询
     *
     * @param id 主键 ID
     * @return 领域模型 Optional
     */
    Optional<StructureDefinition> findById(long id);

    /**
     * 列出全部结构化定义
     */
    List<StructureDefinition> findAll();
}
