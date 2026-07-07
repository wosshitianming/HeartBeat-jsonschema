// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class MpAccountRepositoryImpl implements MpAccountRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpAccountDOMapper accountDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listAccounts() {
        // 注释：设置或计算当前变量值。
        MpAccountDOExample example = new MpAccountDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return accountDOMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toAccountRecord)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord getAccount(String id) {
        // 注释：返回当前处理结果。
        return toAccountRecord(requireAccount(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveAccount(MpAccountRequest request) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        MpAccountDO record = findAccountById(longValue(request.getId(), -1L));
        // 注释：判断当前业务条件。
        if (record == null) {
            // 注释：设置或计算当前变量值。
            record = new MpAccountDO();
            // 注释：执行当前代码行。
            record.setTenantId(tenantId());
            // 注释：执行当前代码行。
            record.setCreateTime(now);
            // 注释：执行当前代码行。
            applyAccount(record, request);
            // 注释：执行当前代码行。
            record.setUpdateTime(now);
            // 注释：执行当前代码行。
            accountDOMapper.insertSelective(record);
            // 注释：返回当前处理结果。
            return toAccountRecord(record);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        applyAccount(record, request);
        // 注释：执行当前代码行。
        record.setUpdateTime(now);
        // 注释：执行当前代码行。
        accountDOMapper.updateByPrimaryKeySelective(record);
        // 注释：返回当前处理结果。
        return toAccountRecord(requireAccount(String.valueOf(record.getId())));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void applyAccount(MpAccountDO record, MpAccountRequest request) {
        // 注释：执行当前代码行。
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "MP Account")));
        // 注释：执行当前代码行。
        record.setAppId(defaultText(request.getAppId(), defaultText(record.getAppId(), "")));
        // 注释：执行当前代码行。
        record.setAppSecret(defaultText(request.getAppSecret(), defaultText(record.getAppSecret(), "")));
        // 注释：执行当前代码行。
        record.setToken(defaultText(request.getToken(), defaultText(record.getToken(), "")));
        // 注释：执行当前代码行。
        record.setAesKey(defaultText(request.getAesKey(), defaultText(record.getAesKey(), "")));
        // 注释：执行当前代码行。
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private MpAccountDO requireAccount(String id) {
        // 注释：设置或计算当前变量值。
        MpAccountDO record = findAccountById(longValue(id, -1L));
        // 注释：判断当前业务条件。
        if (record != null) {
            // 注释：返回当前处理结果。
            return record;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        MpAccountDOExample example = new MpAccountDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(id);
        // 注释：设置或计算当前变量值。
        List<MpAccountDO> records = accountDOMapper.selectByExample(example);
        // 注释：判断当前业务条件。
        if (records.isEmpty()) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("MP account not found: " + id);
            // 注释：结束当前代码块。
        }
        // 注释：返回当前处理结果。
        return records.get(0);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private MpAccountDO findAccountById(long id) {
        // 注释：判断当前业务条件。
        if (id <= 0) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        MpAccountDO record = accountDOMapper.selectByPrimaryKey(id);
        // 注释：返回当前处理结果。
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord toAccountRecord(MpAccountDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", stringValue(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", stringValue(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("appId", entity.getAppId());
        // 注释：执行当前代码行。
        row.put("appSecret", entity.getAppSecret());
        // 注释：执行当前代码行。
        row.put("token", entity.getToken());
        // 注释：执行当前代码行。
        row.put("aesKey", entity.getAesKey());
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("createTime", stringValue(entity.getCreateTime()));
        // 注释：执行当前代码行。
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        // 注释：返回当前处理结果。
        return DomainRecord.of(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String defaultText(String value, String defaultValue) {
        // 注释：返回当前处理结果。
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private long longValue(Object raw, long defaultValue) {
        // 注释：判断当前业务条件。
        if (raw instanceof Number) {
            // 注释：返回当前处理结果。
            return ((Number) raw).longValue();
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
            // 注释：捕获并处理当前异常。
        } catch (NumberFormatException ignored) {
            // 注释：返回当前处理结果。
            return defaultValue;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value).trim();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
