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

    @Resource
    private ObjectMapper objectMapper;

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
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        MobilePageDOExample example = new MobilePageDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAppIdEqualTo(longValue(appId, 0L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("sort_no, id");
        // 返回已经完成封装的业务结果。
        return pageDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::toPageRecord)
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 创建数据库记录对象，承载即将写入的业务字段。
        MobilePageDO record = new MobilePageDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setAppId(longValue(request.getAppId(), 0L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setName(defaultText(request.getName(), "Page"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setPageKey(defaultText(request.getPageKey(), ""));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setRoutePath(defaultText(request.getRoutePath(), ""));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setSchemaJson(jsonValue(request.getSchema()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setStatus(defaultText(request.getStatus(), "DRAFT"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        pageDOMapper.insertSelective(record);
        // 返回已经完成封装的业务结果。
        return toPageRecord(record);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成移动端配置数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord toPageRecord(MobilePageDO entity) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("id", String.valueOf(entity.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("appId", String.valueOf(entity.getAppId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("name", entity.getName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("pageKey", entity.getPageKey());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("routePath", entity.getRoutePath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("schema", readJson(entity.getSchemaJson()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("sortNo", entity.getSortNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("status", entity.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成移动端配置数据访问。
     *
     * @param json 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private JsonNode readJson(String json) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
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
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return StringUtils.isBlank(value) ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
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
