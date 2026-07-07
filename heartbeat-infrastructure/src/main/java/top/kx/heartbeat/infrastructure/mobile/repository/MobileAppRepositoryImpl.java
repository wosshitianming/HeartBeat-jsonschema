// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.mobile.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.port.MobileAppRepository;
import top.kx.heartbeat.application.mobile.request.MobileAppRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobileAppVersionDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileAppDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobileAppVersionDOMapper;
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
public class MobileAppRepositoryImpl implements MobileAppRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobileAppDOMapper appDOMapper;

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobileAppVersionDOMapper versionDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listApps() {
        // 注释：设置或计算当前变量值。
        MobileAppDOExample example = new MobileAppDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return appDOMapper.selectByExampleWithBLOBs(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toAppRecord)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveApp(MobileAppRequest request) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        MobileAppDO record = new MobileAppDO();
        // 注释：执行当前代码行。
        record.setTenantId(tenantId());
        // 注释：执行当前代码行。
        record.setName(defaultText(request.getName(), "Mobile App"));
        // 注释：执行当前代码行。
        record.setAppKey(defaultText(request.getAppKey(), ""));
        // 注释：执行当前代码行。
        record.setEntryUrl(defaultText(request.getEntryUrl(), ""));
        // 注释：执行当前代码行。
        record.setStatus(defaultText(request.getStatus(), "DRAFT"));
        // 注释：执行当前代码行。
        record.setConfigJson(jsonValue(request.getConfig()));
        // 注释：执行当前代码行。
        record.setCreateTime(now);
        // 注释：执行当前代码行。
        record.setUpdateTime(now);
        // 注释：执行当前代码行。
        appDOMapper.insertSelective(record);

        // 注释：判断当前业务条件。
        if ("PUBLISHED".equals(record.getStatus())) {
            // 注释：设置或计算当前变量值。
            MobileAppVersionDO version = new MobileAppVersionDO();
            // 注释：执行当前代码行。
            version.setTenantId(tenantId());
            // 注释：执行当前代码行。
            version.setAppId(record.getId());
            // 注释：执行当前代码行。
            version.setVersionNo(1);
            // 注释：执行当前代码行。
            version.setSchemaJson(record.getConfigJson());
            // 注释：执行当前代码行。
            version.setStatus("PUBLISHED");
            // 注释：执行当前代码行。
            version.setPublishedAt(now);
            // 注释：执行当前代码行。
            version.setCreateTime(now);
            // 注释：执行当前代码行。
            versionDOMapper.insertSelective(version);
            // 注释：结束当前代码块。
        }

        // 注释：返回当前处理结果。
        return toAppRecord(record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord toAppRecord(MobileAppDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("appKey", entity.getAppKey());
        // 注释：执行当前代码行。
        row.put("entryUrl", entity.getEntryUrl());
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("config", readJson(entity.getConfigJson()));
        // 注释：执行当前代码行。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 注释：执行当前代码行。
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        // 注释：返回当前处理结果。
        return DomainRecord.of(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private JsonNode readJson(String json) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("JSON parse failed", ex);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String jsonValue(Object value) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
            // 注释：捕获并处理当前异常。
        } catch (Exception ex) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("JSON serialize failed", ex);
            // 注释：结束当前代码块。
        }
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
    private long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
