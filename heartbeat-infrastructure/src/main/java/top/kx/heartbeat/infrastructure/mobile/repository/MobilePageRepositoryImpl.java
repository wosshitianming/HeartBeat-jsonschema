// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.mobile.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mobile.port.MobilePageRepository;
import top.kx.heartbeat.application.mobile.request.MobilePageRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobilePageDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mobile.MobilePageDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mobile.MobilePageDOMapper;
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
public class MobilePageRepositoryImpl implements MobilePageRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobilePageDOMapper pageDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listPages(String appId) {
        // 注释：设置或计算当前变量值。
        MobilePageDOExample example = new MobilePageDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(longValue(appId, 0L));
        // 注释：执行当前代码行。
        example.setOrderByClause("sort_no, id");
        // 注释：返回当前处理结果。
        return pageDOMapper.selectByExampleWithBLOBs(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toPageRecord)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord savePage(MobilePageRequest request) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        MobilePageDO record = new MobilePageDO();
        // 注释：执行当前代码行。
        record.setTenantId(tenantId());
        // 注释：执行当前代码行。
        record.setAppId(longValue(request.getAppId(), 0L));
        // 注释：执行当前代码行。
        record.setName(defaultText(request.getName(), "Page"));
        // 注释：执行当前代码行。
        record.setPageKey(defaultText(request.getPageKey(), ""));
        // 注释：执行当前代码行。
        record.setRoutePath(defaultText(request.getRoutePath(), ""));
        // 注释：执行当前代码行。
        record.setSchemaJson(jsonValue(request.getSchema()));
        // 注释：设置或计算当前变量值。
        record.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        // 注释：执行当前代码行。
        record.setStatus(defaultText(request.getStatus(), "DRAFT"));
        // 注释：执行当前代码行。
        record.setCreateTime(now);
        // 注释：执行当前代码行。
        record.setUpdateTime(now);
        // 注释：执行当前代码行。
        pageDOMapper.insertSelective(record);
        // 注释：返回当前处理结果。
        return toPageRecord(record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord toPageRecord(MobilePageDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", String.valueOf(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("appId", String.valueOf(entity.getAppId()));
        // 注释：执行当前代码行。
        row.put("name", entity.getName());
        // 注释：执行当前代码行。
        row.put("pageKey", entity.getPageKey());
        // 注释：执行当前代码行。
        row.put("routePath", entity.getRoutePath());
        // 注释：执行当前代码行。
        row.put("schema", readJson(entity.getSchemaJson()));
        // 注释：执行当前代码行。
        row.put("sortNo", entity.getSortNo());
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
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
    private long longValue(String value, long defaultValue) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
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
    private long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
