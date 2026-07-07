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
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PlatformConfigRepositoryImpl implements PlatformConfigRepository {

    @Resource
    private SysConfigDOMapper configMapper;
    @Resource
    private SysDictTypeDOMapper dictTypeMapper;
    @Resource
    private SysDictItemDOMapper dictItemMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listConfigurations() {
        return configMapper.selectByExample(new SysConfigDOExample())
                .stream()
                .map(this::record)
                .collect(Collectors.toList());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createConfiguration(PlatformConfigurationRequest request) {
        SysConfigDO row = configurationRow(request);
        touch(row, true);
        configMapper.insertSelective(row);
        return record(row);
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord updateConfiguration(String id, PlatformConfigurationRequest request) {
        SysConfigDO row = configurationRow(request);
        Integer key = integerValue(id);
        row.setId(key == null ? null : key.longValue());
        touch(row, false);
        configMapper.updateByPrimaryKeySelective(row);
        SysConfigDO persisted = key == null ? null : configMapper.selectByPrimaryKey(key);
        return record(persisted == null ? row : persisted);
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     */
    @Override
    public void deleteConfiguration(String id) {
        Integer key = integerValue(id);
        if (key != null) {
            configMapper.deleteByPrimaryKey(key);
        }
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listDictTypes() {
        return dictTypeMapper.selectByExample(new SysDictTypeDOExample())
                .stream()
                .map(row -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("id", row.getId());
                    values.put("dictCode", row.getDictCode());
                    values.put("dictName", row.getDictName());
                    values.put("description", row.getDescription());
                    values.put("status", row.getStatus());
                    return DomainRecord.of(values);
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listDictData() {
        return dictItemMapper.selectByExample(new SysDictItemDOExample())
                .stream()
                .map(row -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("id", row.getId());
                    values.put("dictTypeId", row.getDictTypeId());
                    values.put("itemLabel", row.getItemLabel());
                    values.put("itemValue", row.getItemValue());
                    values.put("sortNo", row.getSortNo());
                    values.put("status", row.getStatus());
                    return DomainRecord.of(values);
                })
                .collect(Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private SysConfigDO configurationRow(PlatformConfigurationRequest request) {
        PlatformConfigurationRequest safeRequest =
                request == null ? new PlatformConfigurationRequest() : request;
        SysConfigDO row = new SysConfigDO();
        row.setConfigKey(safeRequest.getConfigKey());
        row.setConfigName(safeRequest.getConfigName());
        row.setConfigValue(safeRequest.getConfigValue());
        row.setValueType(safeRequest.getValueType());
        row.setEncrypted(safeRequest.getEncrypted());
        row.setConfigGroup(safeRequest.getConfigGroup());
        row.setDescription(safeRequest.getDescription());
        row.setStatus(safeRequest.getStatus());
        return row;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param creating 是否为新增写入。
     */
    private void touch(SysConfigDO row, boolean creating) {
        Date now = new Date();
        if (creating) {
            row.setTenantId(tenantId());
            row.setCreateTime(now);
            row.setVersion(0);
            row.setDeleteMarker(0L);
            if (row.getStatus() == null) {
                row.setStatus("ENABLED");
            }
        }
        row.setUpdateTime(now);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysConfigDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("configKey", row.getConfigKey());
        values.put("configName", row.getConfigName());
        values.put("configValue", row.getConfigValue());
        values.put("valueType", row.getValueType());
        values.put("encrypted", row.getEncrypted());
        values.put("configGroup", row.getConfigGroup());
        values.put("description", row.getDescription());
        values.put("status", row.getStatus());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
        return DomainRecord.of(values);
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private Integer integerValue(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
