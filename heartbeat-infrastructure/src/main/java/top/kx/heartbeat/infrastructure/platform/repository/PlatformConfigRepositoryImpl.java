package top.kx.heartbeat.infrastructure.platform.repository;

import org.apache.commons.lang3.StringUtils;
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
        SysConfigDOExample example = new SysConfigDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return configMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::record)
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
        Long key = longValue(id);
        if (key == null) {
            throw new IllegalArgumentException("Invalid configuration id: " + id);
        }
        row.setId(key);
        touch(row, false);
        SysConfigDO persisted = null;
        if (key != null) {
            SysConfigDOExample example = configurationById(key);
            if (configMapper.updateByExampleSelective(row, example) == 0) {
                throw new IllegalArgumentException("Configuration does not exist: " + id);
            }
            persisted = first(configMapper.selectByExample(example));
        }
        return record(persisted == null ? row : persisted);
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     */
    @Override
    public void deleteConfiguration(String id) {
        Long key = longValue(id);
        if (key != null) {
            configMapper.deleteByExample(configurationById(key));
        }
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listDictTypes() {
        SysDictTypeDOExample example = new SysDictTypeDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return dictTypeMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(row -> {
                    // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
                    Map<String, Object> values = new LinkedHashMap<>();
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("id", row.getId());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("dictCode", row.getDictCode());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("dictName", row.getDictName());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("description", row.getDescription());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("status", row.getStatus());
                    // 返回已经完成封装的业务结果。
                    return DomainRecord.of(values);
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                })
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listDictData() {
        SysDictItemDOExample example = new SysDictItemDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return dictItemMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(row -> {
                    // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
                    Map<String, Object> values = new LinkedHashMap<>();
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("id", row.getId());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("dictTypeId", row.getDictTypeId());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("itemLabel", row.getItemLabel());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("itemValue", row.getItemValue());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("sortNo", row.getSortNo());
                    // 写入对外字段，保持调用方依赖的响应结构稳定。
                    values.put("status", row.getStatus());
                    // 返回已经完成封装的业务结果。
                    return DomainRecord.of(values);
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                })
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private SysConfigDO configurationRow(PlatformConfigurationRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PlatformConfigurationRequest safeRequest =
                // 创建下游写入请求对象，集中承载本次业务处理结果。
                request == null ? new PlatformConfigurationRequest() : request;
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysConfigDO row = new SysConfigDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setConfigKey(safeRequest.getConfigKey());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setConfigName(safeRequest.getConfigName());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setConfigValue(safeRequest.getConfigValue());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setValueType(safeRequest.getValueType());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setEncrypted(safeRequest.getEncrypted());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setConfigGroup(safeRequest.getConfigGroup());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setDescription(safeRequest.getDescription());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(safeRequest.getStatus());
        // 返回已经完成封装的业务结果。
        return row;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param creating 是否为新增写入。
     */
    private void touch(SysConfigDO row, boolean creating) {
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 根据当前业务条件选择对应处理路径。
        if (creating) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setTenantId(tenantId());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setCreateTime(now);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setVersion(0);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setDeleteMarker(0L);
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (row.getStatus() == null) {
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                row.setStatus("ENABLED");
            }
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysConfigDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row == null) {
            // 返回已经完成封装的业务结果。
            return DomainRecord.of(values);
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("configKey", row.getConfigKey());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("configName", row.getConfigName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("configValue", row.getConfigValue());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("valueType", row.getValueType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("encrypted", row.getEncrypted());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("configGroup", row.getConfigGroup());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("description", row.getDescription());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", row.getUpdateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        return TenantContext.getRequiredTenantId();
    }

    private SysConfigDOExample configurationById(Long id) {
        SysConfigDOExample example = new SysConfigDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdEqualTo(id)
                .andDeleteMarkerEqualTo(0L);
        return example;
    }

    private <T> T first(List<T> rows) {
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private Long longValue(String value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return StringUtils.isBlank(value) ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return null;
        }
    }
}
