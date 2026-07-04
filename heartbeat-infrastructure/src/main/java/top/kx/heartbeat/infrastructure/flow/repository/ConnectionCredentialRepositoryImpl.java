package top.kx.heartbeat.infrastructure.flow.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.flow.model.ConnectionCredential;
import top.kx.heartbeat.domain.flow.repository.ConnectionCredentialRepository;
import top.kx.heartbeat.infrastructure.flow.convert.ConnectionCredentialConvert;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbConnectionCredentialDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbConnectionCredentialDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbConnectionCredentialDOMapper;

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
     * 默认租户标识。
     */
    private static final long DEFAULT_TENANT_ID = 1L;

    /**
     * 默认操作人标识。
     */
    private static final long DEFAULT_OPERATOR_ID = 1L;

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
        example.createCriteria().andTenantIdEqualTo(DEFAULT_TENANT_ID);
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
        HbConnectionCredentialDOWithBLOBs row = mapper.selectByPrimaryKey(parseLong(id));
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
        // 转换为持久化对象。
        HbConnectionCredentialDOWithBLOBs row = convert.toEntity(credential);
        // 补齐租户和审计字段。
        fillAudit(row);
        // 判断主键是否存在。
        if (row.getId() == null) {
            // 插入连接凭据。
            mapper.insertSelective(row);
        } else {
            // 更新连接凭据。
            mapper.updateByPrimaryKeySelective(row);
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
        mapper.deleteByPrimaryKey(parseLong(id));
    }

    /**
     * 补齐审计字段。
     *
     * @param row 连接凭据持久化对象
     */
    private void fillAudit(HbConnectionCredentialDOWithBLOBs row) {
        // 获取当前时间。
        Date now = new Date();
        // 写入默认租户。
        row.setTenantId(DEFAULT_TENANT_ID);
        // 写入创建人。
        row.setCreateBy(DEFAULT_OPERATOR_ID);
        // 写入更新人。
        row.setUpdateBy(DEFAULT_OPERATOR_ID);
        // 写入创建时间。
        row.setCreateTime(row.getCreateTime() == null ? now : row.getCreateTime());
        // 写入更新时间。
        row.setUpdateTime(now);
    }

    /**
     * 解析字符串主键。
     *
     * @param value 字符串主键
     * @return Long 主键
     */
    private Long parseLong(String value) {
        // 判断字符串是否为空。
        if (value == null || value.trim().isEmpty()) {
            // 返回空主键。
            return null;
        }
        // 返回 Long 主键。
        return Long.valueOf(value);
    }
}
