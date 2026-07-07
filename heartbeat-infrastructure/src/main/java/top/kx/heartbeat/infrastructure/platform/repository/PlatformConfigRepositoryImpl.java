// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformConfigRepository;
import top.kx.heartbeat.application.platform.request.PlatformConfigurationRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysConfigDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysConfigDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDictItemDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysDictTypeDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysConfigDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDictItemDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysDictTypeDOMapper;
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
public class PlatformConfigRepositoryImpl implements PlatformConfigRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysConfigDOMapper configMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysDictTypeDOMapper dictTypeMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysDictItemDOMapper dictItemMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listConfigurations() {
        // 注释：返回当前处理结果。
        return configMapper.selectByExample(new SysConfigDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::record)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createConfiguration(PlatformConfigurationRequest request) {
        // 注释：设置或计算当前变量值。
        SysConfigDO row = configurationRow(request);
        // 注释：执行当前代码行。
        touch(row, true);
        // 注释：执行当前代码行。
        configMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return record(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateConfiguration(String id, PlatformConfigurationRequest request) {
        // 注释：设置或计算当前变量值。
        SysConfigDO row = configurationRow(request);
        // 注释：设置或计算当前变量值。
        Integer key = integerValue(id);
        // 注释：设置或计算当前变量值。
        row.setId(key == null ? null : key.longValue());
        // 注释：执行当前代码行。
        touch(row, false);
        // 注释：执行当前代码行。
        configMapper.updateByPrimaryKeySelective(row);
        // 注释：设置或计算当前变量值。
        SysConfigDO persisted = key == null ? null : configMapper.selectByPrimaryKey(key);
        // 注释：返回当前处理结果。
        return record(persisted == null ? row : persisted);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void deleteConfiguration(String id) {
        // 注释：设置或计算当前变量值。
        Integer key = integerValue(id);
        // 注释：判断当前业务条件。
        if (key != null) {
            // 注释：执行当前代码行。
            configMapper.deleteByPrimaryKey(key);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listDictTypes() {
        // 注释：返回当前处理结果。
        return dictTypeMapper.selectByExample(new SysDictTypeDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(row -> {
                    // 注释：设置或计算当前变量值。
                    Map<String, Object> values = new LinkedHashMap<>();
                    // 注释：执行当前代码行。
                    values.put("id", row.getId());
                    // 注释：执行当前代码行。
                    values.put("dictCode", row.getDictCode());
                    // 注释：执行当前代码行。
                    values.put("dictName", row.getDictName());
                    // 注释：执行当前代码行。
                    values.put("description", row.getDescription());
                    // 注释：执行当前代码行。
                    values.put("status", row.getStatus());
                    // 注释：返回当前处理结果。
                    return DomainRecord.of(values);
                    // 注释：执行当前代码行。
                })
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listDictData() {
        // 注释：返回当前处理结果。
        return dictItemMapper.selectByExample(new SysDictItemDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(row -> {
                    // 注释：设置或计算当前变量值。
                    Map<String, Object> values = new LinkedHashMap<>();
                    // 注释：执行当前代码行。
                    values.put("id", row.getId());
                    // 注释：执行当前代码行。
                    values.put("dictTypeId", row.getDictTypeId());
                    // 注释：执行当前代码行。
                    values.put("itemLabel", row.getItemLabel());
                    // 注释：执行当前代码行。
                    values.put("itemValue", row.getItemValue());
                    // 注释：执行当前代码行。
                    values.put("sortNo", row.getSortNo());
                    // 注释：执行当前代码行。
                    values.put("status", row.getStatus());
                    // 注释：返回当前处理结果。
                    return DomainRecord.of(values);
                    // 注释：执行当前代码行。
                })
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private SysConfigDO configurationRow(PlatformConfigurationRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformConfigurationRequest safeRequest =
                // 注释：设置或计算当前变量值。
                request == null ? new PlatformConfigurationRequest() : request;
        // 注释：设置或计算当前变量值。
        SysConfigDO row = new SysConfigDO();
        // 注释：执行当前代码行。
        row.setConfigKey(safeRequest.getConfigKey());
        // 注释：执行当前代码行。
        row.setConfigName(safeRequest.getConfigName());
        // 注释：执行当前代码行。
        row.setConfigValue(safeRequest.getConfigValue());
        // 注释：执行当前代码行。
        row.setValueType(safeRequest.getValueType());
        // 注释：执行当前代码行。
        row.setEncrypted(safeRequest.getEncrypted());
        // 注释：执行当前代码行。
        row.setConfigGroup(safeRequest.getConfigGroup());
        // 注释：执行当前代码行。
        row.setDescription(safeRequest.getDescription());
        // 注释：执行当前代码行。
        row.setStatus(safeRequest.getStatus());
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void touch(SysConfigDO row, boolean creating) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：判断当前业务条件。
        if (creating) {
            // 注释：执行当前代码行。
            row.setTenantId(tenantId());
            // 注释：执行当前代码行。
            row.setCreateTime(now);
            // 注释：执行当前代码行。
            row.setVersion(0);
            // 注释：执行当前代码行。
            row.setDeleteMarker(0L);
            // 注释：判断当前业务条件。
            if (row.getStatus() == null) {
                // 注释：执行当前代码行。
                row.setStatus("ENABLED");
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(SysConfigDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：判断当前业务条件。
        if (row == null) {
            // 注释：返回当前处理结果。
            return DomainRecord.of(values);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("configKey", row.getConfigKey());
        // 注释：执行当前代码行。
        values.put("configName", row.getConfigName());
        // 注释：执行当前代码行。
        values.put("configValue", row.getConfigValue());
        // 注释：执行当前代码行。
        values.put("valueType", row.getValueType());
        // 注释：执行当前代码行。
        values.put("encrypted", row.getEncrypted());
        // 注释：执行当前代码行。
        values.put("configGroup", row.getConfigGroup());
        // 注释：执行当前代码行。
        values.put("description", row.getDescription());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：执行当前代码行。
        values.put("updateTime", row.getUpdateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
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

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Integer integerValue(String value) {
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return value == null || value.trim().isEmpty() ? null : Integer.parseInt(value.trim());
            // 注释：捕获并处理当前异常。
        } catch (NumberFormatException ignored) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
