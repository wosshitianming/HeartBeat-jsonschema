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
        // 创建数据库记录对象，承载即将写入的业务字段。
        StructurePublishAuditDO row = new StructurePublishAuditDO();
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(currentTenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDefinitionId(definitionId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setVersionNo(versionNo);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setOperatorId(operatorId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(status);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setSummary(summary);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCreateBy(String.valueOf(operatorId));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateBy(String.valueOf(operatorId));
        // 将当前业务变更写入持久化层，保持数据状态同步。
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
