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

@Repository
public class PlatformConfigRepositoryImpl implements PlatformConfigRepository {

    @Resource
    private SysConfigDOMapper configMapper;
    @Resource
    private SysDictTypeDOMapper dictTypeMapper;
    @Resource
    private SysDictItemDOMapper dictItemMapper;

    @Override
    public List<DomainRecord> listConfigurations() {
        return configMapper.selectByExample(new SysConfigDOExample())
                .stream()
                .map(this::record)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord createConfiguration(PlatformConfigurationRequest request) {
        SysConfigDO row = configurationRow(request);
        touch(row, true);
        configMapper.insertSelective(row);
        return record(row);
    }

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

    @Override
    public void deleteConfiguration(String id) {
        Integer key = integerValue(id);
        if (key != null) {
            configMapper.deleteByPrimaryKey(key);
        }
    }

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

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Integer integerValue(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
