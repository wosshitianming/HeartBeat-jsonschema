// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.mp.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.port.MpMaterialRepository;
import top.kx.heartbeat.application.mp.request.MpMaterialRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMaterialDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpMaterialDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpMaterialDOMapper;
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
public class MpMaterialRepositoryImpl implements MpMaterialRepository {

    // 注释：声明当前成员或方法。
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpMaterialDOMapper materialDOMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listMaterials(String accountId) {
        // 注释：设置或计算当前变量值。
        MpMaterialDOExample example = new MpMaterialDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return materialDOMapper.selectByExampleWithBLOBs(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::toMaterialRecord)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveMaterial(MpMaterialRequest request) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        MpMaterialDO record = findMaterialById(longValue(request.getId(), -1L));
        // 注释：判断当前业务条件。
        if (record == null) {
            // 注释：设置或计算当前变量值。
            record = new MpMaterialDO();
            // 注释：执行当前代码行。
            record.setTenantId(tenantId());
            // 注释：执行当前代码行。
            record.setCreateTime(now);
            // 注释：执行当前代码行。
            applyMaterial(record, request);
            // 注释：执行当前代码行。
            record.setUpdateTime(now);
            // 注释：执行当前代码行。
            materialDOMapper.insertSelective(record);
            // 注释：返回当前处理结果。
            return toMaterialRecord(record);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        applyMaterial(record, request);
        // 注释：执行当前代码行。
        record.setUpdateTime(now);
        // 注释：执行当前代码行。
        materialDOMapper.updateByPrimaryKeySelective(record);
        // 注释：返回当前处理结果。
        return toMaterialRecord(record);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void applyMaterial(MpMaterialDO record, MpMaterialRequest request) {
        // 注释：执行当前代码行。
        record.setAccountId(longValue(defaultText(request.getAccountId(), stringValue(record.getAccountId())), 0L));
        // 注释：执行当前代码行。
        record.setMaterialType(defaultText(request.getMaterialType(), defaultText(record.getMaterialType(), "text")));
        // 注释：执行当前代码行。
        record.setTitle(defaultText(request.getTitle(), defaultText(record.getTitle(), "Material")));
        // 注释：执行当前代码行。
        record.setMediaId(defaultText(request.getMediaId(), defaultText(record.getMediaId(), "")));
        // 注释：执行当前代码行。
        record.setUrl(defaultText(request.getUrl(), defaultText(record.getUrl(), "")));
        // 注释：执行当前代码行。
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        // 注释：设置或计算当前变量值。
        record.setPayload(request.getPayload() == null ? defaultJson(record.getPayload()) : jsonValue(request.getPayload()));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private MpMaterialDO findMaterialById(long id) {
        // 注释：判断当前业务条件。
        if (id <= 0) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        MpMaterialDO record = materialDOMapper.selectByPrimaryKey(id);
        // 注释：返回当前处理结果。
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord toMaterialRecord(MpMaterialDO entity) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> row = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        row.put("id", stringValue(entity.getId()));
        // 注释：执行当前代码行。
        row.put("tenantId", stringValue(entity.getTenantId()));
        // 注释：执行当前代码行。
        row.put("accountId", stringValue(entity.getAccountId()));
        // 注释：执行当前代码行。
        row.put("materialType", entity.getMaterialType());
        // 注释：执行当前代码行。
        row.put("title", entity.getTitle());
        // 注释：执行当前代码行。
        row.put("mediaId", entity.getMediaId());
        // 注释：执行当前代码行。
        row.put("url", entity.getUrl());
        // 注释：执行当前代码行。
        row.put("status", entity.getStatus());
        // 注释：执行当前代码行。
        row.put("payload", readJson(entity.getPayload()));
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
            // 注释：判断当前业务条件。
            if (value instanceof String) {
                // 注释：设置或计算当前变量值。
                String text = ((String) value).trim();
                // 注释：返回当前处理结果。
                return StringUtils.isBlank(text) ? "{}" : text;
                // 注释：结束当前代码块。
            }
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
    private String defaultJson(String value) {
        // 注释：返回当前处理结果。
        return StringUtils.isBlank(value) ? "{}" : value;
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
