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

@Repository
public class MpAccountRepositoryImpl implements MpAccountRepository {

    @Resource
    private MpAccountDOMapper accountDOMapper;

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

    @Override
    public DomainRecord getAccount(String id) {
        return toAccountRecord(requireAccount(id));
    }

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

    private void applyAccount(MpAccountDO record, MpAccountRequest request) {
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "MP Account")));
        record.setAppId(defaultText(request.getAppId(), defaultText(record.getAppId(), "")));
        record.setAppSecret(defaultText(request.getAppSecret(), defaultText(record.getAppSecret(), "")));
        record.setToken(defaultText(request.getToken(), defaultText(record.getToken(), "")));
        record.setAesKey(defaultText(request.getAesKey(), defaultText(record.getAesKey(), "")));
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
    }

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

    private MpAccountDO findAccountById(long id) {
        if (id <= 0) {
            return null;
        }
        MpAccountDO record = accountDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

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

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
