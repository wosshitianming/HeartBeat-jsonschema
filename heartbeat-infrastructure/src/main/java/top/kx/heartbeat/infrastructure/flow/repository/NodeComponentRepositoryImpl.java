package top.kx.heartbeat.infrastructure.flow.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;
import top.kx.heartbeat.domain.flow.model.NodeComponentStatus;
import top.kx.heartbeat.domain.flow.repository.NodeComponentRepository;
import top.kx.heartbeat.infrastructure.flow.convert.NodeComponentConvert;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbNodeComponentDO;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbNodeComponentDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbNodeComponentDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 节点组件仓储实现。
 *
 * <p>用于通过 MyBatis Generator Mapper 持久化节点组件清单。</p>
 */
@Repository
public class NodeComponentRepositoryImpl implements NodeComponentRepository {

    /**
     * 默认操作人标识。
     */
    private static final String DEFAULT_OPERATOR_ID = "1";

    /**
     * 节点组件 Mapper。
     */
    @Resource
    private HbNodeComponentDOMapper mapper;

    /**
     * 节点组件结构转换器。
     */
    @Resource
    private NodeComponentConvert convert;

    /**
     * 查询所有启用的节点组件。
     *
     * @return 启用节点组件列表
     */
    @Override
    public List<NodeComponentManifest> findAllActive() {
        // 创建查询条件。
        HbNodeComponentDOExample example = new HbNodeComponentDOExample();
        // 添加租户和状态条件。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andStatusEqualTo(NodeComponentStatus.ACTIVE.getCode());
        // 设置排序规则。
        example.setOrderByClause("sort_no ASC, id ASC");
        // 查询并转换为领域模型。
        return mapper.selectByExampleWithBLOBs(example).stream().map(convert::toDomain).collect(Collectors.toList());
    }

    /**
     * 按组件类型和版本查询节点组件。
     *
     * @param type 组件类型
     * @param version 组件版本
     * @return 节点组件清单
     */
    @Override
    public Optional<NodeComponentManifest> findByTypeAndVersion(String type, String version) {
        long tenantId = tenantId();
        // 创建查询条件。
        HbNodeComponentDOExample example = new HbNodeComponentDOExample();
        // 添加租户、类型和版本条件。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andTypeEqualTo(type)
                .andVersionEqualTo(version);
        // 查询节点组件。
        List<HbNodeComponentDO> rows = mapper.selectByExampleWithBLOBs(example);
        // 返回查询结果。
        return rows.isEmpty() ? Optional.empty() : Optional.of(convert.toDomain(rows.get(0)));
    }

    /**
     * 保存或更新节点组件。
     *
     * @param manifest 节点组件清单
     * @return 保存后的节点组件清单
     */
    @Override
    public NodeComponentManifest save(NodeComponentManifest manifest) {
        long tenantId = tenantId();
        // 转换为持久化对象。
        HbNodeComponentDO row = convert.toEntity(manifest);
        // 查询同类型同版本组件。
        HbNodeComponentDO existing = selectByTypeAndVersion(manifest.getType(), manifest.getVersion(), tenantId);
        // 补齐租户和审计字段。
        fillAudit(row, existing, tenantId);
        // 判断是否需要更新已有组件。
        if (existing != null) {
            // 写入已有组件主键。
            row.setId(existing.getId());
            // 更新已有组件。
            mapper.updateByExampleSelective(row, componentById(existing.getId(), tenantId));
            // 返回更新后的组件。
            return findByTypeAndVersion(manifest.getType(), manifest.getVersion()).orElse(manifest);
        }
        // 插入新组件。
        mapper.insertSelective(row);
        // 返回保存后的组件。
        return findByTypeAndVersion(manifest.getType(), manifest.getVersion()).orElse(manifest);
    }

    /**
     * 补齐审计字段。
     *
     * @param row 节点组件持久化对象
     */
    private void fillAudit(HbNodeComponentDO row, HbNodeComponentDO existing, long tenantId) {
        // 获取当前时间。
        Date now = new Date();
        // 写入当前租户。
        row.setTenantId(tenantId);
        // 写入创建人。
        row.setCreateBy(existing == null ? DEFAULT_OPERATOR_ID : existing.getCreateBy());
        // 写入更新人。
        row.setUpdateBy(DEFAULT_OPERATOR_ID);
        // 写入创建时间。
        row.setCreateTime(existing == null ? now : existing.getCreateTime());
        // 写入更新时间。
        row.setUpdateTime(now);
    }

    private HbNodeComponentDO selectByTypeAndVersion(String type, String version, long tenantId) {
        HbNodeComponentDOExample example = new HbNodeComponentDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andTypeEqualTo(type)
                .andVersionEqualTo(version);
        List<HbNodeComponentDO> rows = mapper.selectByExampleWithBLOBs(example);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private HbNodeComponentDOExample componentById(Long id, long tenantId) {
        HbNodeComponentDOExample example = new HbNodeComponentDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andIdEqualTo(id);
        return example;
    }

    private long tenantId() {
        return TenantContext.getRequiredTenantId();
    }
}
