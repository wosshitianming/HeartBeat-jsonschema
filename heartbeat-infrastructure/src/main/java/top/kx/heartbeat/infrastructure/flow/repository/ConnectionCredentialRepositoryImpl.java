package top.kx.heartbeat.infrastructure.flow.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.flow.model.ConnectionCredential;
import top.kx.heartbeat.domain.flow.repository.ConnectionCredentialRepository;
import top.kx.heartbeat.infrastructure.flow.convert.ConnectionCredentialConvert;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbConnectionCredentialDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbConnectionCredentialDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbConnectionCredentialDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 连接凭据仓储实现。
 *
 * <p>用于通过 MyBatis Generator Mapper 持久化连接配置和加密后的敏感配置。</p>
 */
@Repository
public class ConnectionCredentialRepositoryImpl implements ConnectionCredentialRepository {

    /**
     * 默认操作人标识。
     */
    private static final String DEFAULT_OPERATOR_ID = "1";

    /**
     * 连接凭据 Mapper。
     */
    @Resource
    private HbConnectionCredentialDOMapper mapper;

    /**
     * 连接凭据结构转换器。
     */
    @Resource
    private ConnectionCredentialConvert convert;

    /**
     * 查询全部连接凭据。
     *
     * @return 连接凭据列表
     */
    @Override
    public List<ConnectionCredential> findAll() {
        // 创建查询条件。
        HbConnectionCredentialDOExample example = new HbConnectionCredentialDOExample();
        // 添加租户条件。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 设置排序规则。
        example.setOrderByClause("update_time DESC, id DESC");
        // 查询并转换为脱敏领域对象。
        return mapper.selectByExampleWithBLOBs(example).stream().map(convert::toMaskedDomain).collect(Collectors.toList());
    }

    /**
     * 按主键查询连接凭据。
     *
     * @param id 连接凭据标识
     * @return 连接凭据
     */
    @Override
    public Optional<ConnectionCredential> findById(String id) {
        // 查询持久化对象。
        HbConnectionCredentialDOWithBLOBs row = selectById(parseLong(id), tenantId());
        // 返回脱敏领域对象。
        return Optional.ofNullable(row).map(convert::toMaskedDomain);
    }

    /**
     * 保存或更新连接凭据。
     *
     * @param credential 连接凭据
     * @return 保存后的连接凭据
     */
    @Override
    public ConnectionCredential save(ConnectionCredential credential) {
        long tenantId = tenantId();
        // 转换为持久化对象。
        HbConnectionCredentialDOWithBLOBs row = convert.toEntity(credential);
        // 判断主键是否存在。
        if (row.getId() == null) {
            // 补齐租户和审计字段。
            fillAudit(row, null, tenantId);
            // 插入连接凭据。
            mapper.insertSelective(row);
        } else {
            HbConnectionCredentialDOWithBLOBs existing = selectById(row.getId(), tenantId);
            if (existing == null) {
                throw new IllegalStateException("连接凭据不存在或不属于当前租户: " + row.getId());
            }
            // 补齐租户和审计字段。
            fillAudit(row, existing, tenantId);
            // 更新连接凭据。
            mapper.updateByExampleSelective(row, credentialById(row.getId(), tenantId));
        }
        // 返回保存后的连接凭据。
        return row.getId() == null ? credential : findById(String.valueOf(row.getId())).orElse(credential);
    }

    /**
     * 删除连接凭据。
     *
     * @param id 连接凭据标识
     */
    @Override
    public void delete(String id) {
        // 删除连接凭据。
        Long credentialId = parseLong(id);
        if (credentialId != null) {
            mapper.deleteByExample(credentialById(credentialId, tenantId()));
        }
    }

    /**
     * 补齐审计字段。
     *
     * @param row 连接凭据持久化对象
     */
    private void fillAudit(HbConnectionCredentialDOWithBLOBs row,
                           HbConnectionCredentialDOWithBLOBs existing,
                           long tenantId) {
        // 获取当前时间。
        Date now = new Date();
        // 写入当前租户。
        row.setTenantId(tenantId);
        // 写入创建人。
        row.setCreateBy(existing == null ? DEFAULT_OPERATOR_ID : existing.getCreateBy());
        // 写入更新人。
        row.setUpdateBy(DEFAULT_OPERATOR_ID);
        // 写入创建时间。
        row.setCreateTime(existing == null
                ? (row.getCreateTime() == null ? now : row.getCreateTime())
                : existing.getCreateTime());
        // 写入更新时间。
        row.setUpdateTime(now);
    }

    private HbConnectionCredentialDOWithBLOBs selectById(Long id, long tenantId) {
        if (id == null) {
            return null;
        }
        List<HbConnectionCredentialDOWithBLOBs> rows = mapper.selectByExampleWithBLOBs(credentialById(id, tenantId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private HbConnectionCredentialDOExample credentialById(Long id, long tenantId) {
        HbConnectionCredentialDOExample example = new HbConnectionCredentialDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andIdEqualTo(id);
        return example;
    }

    private long tenantId() {
        return TenantContext.getRequiredTenantId();
    }

    /**
     * 解析字符串主键。
     *
     * @param value 字符串主键
     * @return Long 主键
     */
    private Long parseLong(String value) {
        // 判断字符串是否为空。
        if (StringUtils.isBlank(value)) {
            // 返回空主键。
            return null;
        }
        // 返回 Long 主键。
        return Long.valueOf(value);
    }
}
