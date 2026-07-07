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
 * 实现移动端配置持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class MobilePageRepositoryImpl implements MobilePageRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MobilePageDOMapper pageDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成移动端配置数据访问。
     *
     * @param appId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listPages(String appId) {
        MobilePageDOExample example = new MobilePageDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(longValue(appId, 0L));
        example.setOrderByClause("sort_no, id");
        return pageDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toPageRecord)
                .collect(Collectors.toList());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成移动端配置数据访问。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord savePage(MobilePageRequest request) {
        Date now = new Date();
        MobilePageDO record = new MobilePageDO();
        record.setTenantId(tenantId());
        record.setAppId(longValue(request.getAppId(), 0L));
        record.setName(defaultText(request.getName(), "Page"));
        record.setPageKey(defaultText(request.getPageKey(), ""));
        record.setRoutePath(defaultText(request.getRoutePath(), ""));
        record.setSchemaJson(jsonValue(request.getSchema()));
        record.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        record.setStatus(defaultText(request.getStatus(), "DRAFT"));
        record.setCreateTime(now);
        record.setUpdateTime(now);
        pageDOMapper.insertSelective(record);
        return toPageRecord(record);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成移动端配置数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord toPageRecord(MobilePageDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entity.getId()));
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        row.put("appId", String.valueOf(entity.getAppId()));
        row.put("name", entity.getName());
        row.put("pageKey", entity.getPageKey());
        row.put("routePath", entity.getRoutePath());
        row.put("schema", readJson(entity.getSchemaJson()));
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成移动端配置数据访问。
     *
     * @param json 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON parse failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成移动端配置数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String jsonValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成移动端配置数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成移动端配置数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private long longValue(String value, long defaultValue) {
        try {
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成移动端配置数据访问。
     *
     * @return 处理后的业务结果。
     */
    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
