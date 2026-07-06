package top.kx.heartbeat.infrastructure.structure;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.structure.repository.StructurePublishAuditRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructurePublishAuditDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructurePublishAuditDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 结构定义发布审计仓储实现。
 * <p>
 * 负责把应用层发布动作转换为 MBG 生成的审计 DO 并写入结构发布审计表。
 * </p>
 */
@Repository
public class StructurePublishAuditRepositoryImpl implements StructurePublishAuditRepository {

    /**
     * 默认租户标识。
     */
    private static final long DEFAULT_TENANT_ID = 1L;

    /**
     * 结构发布审计 MBG Mapper。
     */
    @Resource
    private StructurePublishAuditDOMapper publishAuditMapper;

    /**
     * 记录一次结构定义发布审计。
     *
     * @param definitionId 结构定义标识
     * @param versionNo    发布版本号
     * @param operatorId   操作人标识
     * @param status       发布状态
     * @param summary      发布摘要
     */
    @Override
    public void record(long definitionId, int versionNo, long operatorId, String status, String summary) {
        StructurePublishAuditDO row = new StructurePublishAuditDO();
        Date now = new Date();
        row.setTenantId(currentTenantId());
        row.setDefinitionId(definitionId);
        row.setVersionNo(versionNo);
        row.setOperatorId(operatorId);
        row.setStatus(status);
        row.setSummary(summary);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setCreateBy(String.valueOf(operatorId));
        row.setUpdateBy(String.valueOf(operatorId));
        publishAuditMapper.insertSelective(row);
    }

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户标识
     */
    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
