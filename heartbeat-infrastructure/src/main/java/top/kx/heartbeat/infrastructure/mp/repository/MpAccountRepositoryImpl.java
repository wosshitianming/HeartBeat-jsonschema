package top.kx.heartbeat.infrastructure.mp.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.port.MpAccountRepository;
import top.kx.heartbeat.application.mp.request.MpAccountRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAccountDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAccountDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpAccountDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class MpAccountRepositoryImpl implements MpAccountRepository {

    @Resource
    private MpAccountDOMapper accountDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listAccounts() {
        MpAccountDOExample example = new MpAccountDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return accountDOMapper.selectByExample(example)
                .stream()
                .map(this::toAccountRecord)
                .collect(Collectors.toList());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord getAccount(String id) {
        return toAccountRecord(requireAccount(id));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord saveAccount(MpAccountRequest request) {
        Date now = new Date();
        MpAccountDO record = findAccountById(longValue(request.getId(), -1L));
        if (record == null) {
            record = new MpAccountDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            applyAccount(record, request);
            record.setUpdateTime(now);
            accountDOMapper.insertSelective(record);
            return toAccountRecord(record);
        }
        applyAccount(record, request);
        record.setUpdateTime(now);
        accountDOMapper.updateByPrimaryKeySelective(record);
        return toAccountRecord(requireAccount(String.valueOf(record.getId())));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param record 应用层业务记录。
     * @param request 公众号管理请求参数。
     */
    private void applyAccount(MpAccountDO record, MpAccountRequest request) {
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "MP Account")));
        record.setAppId(defaultText(request.getAppId(), defaultText(record.getAppId(), "")));
        record.setAppSecret(defaultText(request.getAppSecret(), defaultText(record.getAppSecret(), "")));
        record.setToken(defaultText(request.getToken(), defaultText(record.getToken(), "")));
        record.setAesKey(defaultText(request.getAesKey(), defaultText(record.getAesKey(), "")));
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private MpAccountDO requireAccount(String id) {
        MpAccountDO record = findAccountById(longValue(id, -1L));
        if (record != null) {
            return record;
        }
        MpAccountDOExample example = new MpAccountDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(id);
        List<MpAccountDO> records = accountDOMapper.selectByExample(example);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("MP account not found: " + id);
        }
        return records.get(0);
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private MpAccountDO findAccountById(long id) {
        if (id <= 0) {
            return null;
        }
        MpAccountDO record = accountDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord toAccountRecord(MpAccountDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("name", entity.getName());
        row.put("appId", entity.getAppId());
        row.put("appSecret", entity.getAppSecret());
        row.put("token", entity.getToken());
        row.put("aesKey", entity.getAesKey());
        row.put("status", entity.getStatus());
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private long longValue(Object raw, long defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
