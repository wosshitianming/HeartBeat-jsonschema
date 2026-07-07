package top.kx.heartbeat.infrastructure.mp.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.port.MpMenuRepository;
import top.kx.heartbeat.application.mp.request.MpMenuRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMenuDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMenuDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpMenuDOMapper;
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
public class MpMenuRepositoryImpl implements MpMenuRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private MpMenuDOMapper menuDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listMenus(String accountId) {
        MpMenuDOExample example = new MpMenuDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        example.setOrderByClause("sort_no ASC, id ASC");
        return menuDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toMenuRecord)
                .collect(Collectors.toList());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord saveMenu(MpMenuRequest request) {
        Date now = new Date();
        MpMenuDO record = findMenuById(longValue(request.getId(), -1L));
        if (record == null) {
            record = new MpMenuDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            applyMenu(record, request);
            record.setUpdateTime(now);
            menuDOMapper.insertSelective(record);
            return toMenuRecord(record);
        }
        applyMenu(record, request);
        record.setUpdateTime(now);
        menuDOMapper.updateByPrimaryKeySelective(record);
        return toMenuRecord(record);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param record 应用层业务记录。
     * @param request 公众号管理请求参数。
     */
    private void applyMenu(MpMenuDO record, MpMenuRequest request) {
        record.setAccountId(longValue(defaultText(request.getAccountId(), stringValue(record.getAccountId())), 0L));
        record.setParentId(longValue(defaultText(request.getParentId(), stringValue(record.getParentId())), 0L));
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Menu")));
        record.setMenuType(defaultText(request.getMenuType(), defaultText(record.getMenuType(), "view")));
        record.setUrl(defaultText(request.getUrl(), defaultText(record.getUrl(), "")));
        record.setSortNo(request.getSortNo() == null ? intValue(record.getSortNo(), 0) : request.getSortNo());
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        record.setPayload(request.getPayload() == null ? defaultJson(record.getPayload()) : jsonValue(request.getPayload()));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private MpMenuDO findMenuById(long id) {
        if (id <= 0) {
            return null;
        }
        MpMenuDO record = menuDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord toMenuRecord(MpMenuDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("accountId", stringValue(entity.getAccountId()));
        row.put("parentId", stringValue(entity.getParentId()));
        row.put("name", entity.getName());
        row.put("menuType", entity.getMenuType());
        row.put("url", entity.getUrl());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("payload", readJson(entity.getPayload()));
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String jsonValue(Object value) {
        try {
            if (value instanceof String) {
                String text = ((String) value).trim();
                return StringUtils.isBlank(text) ? "{}" : text;
            }
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String defaultJson(String value) {
        return StringUtils.isBlank(value) ? "{}" : value;
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
    private int intValue(Object raw, int defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return raw == null ? defaultValue : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
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
