package top.kx.heartbeat.infrastructure.report.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.port.ReportDatasetRepository;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportDatasetDO;
import top.kx.heartbeat.infrastructure.persistence.entity.report.ReportDatasetDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.report.ReportDatasetDOMapper;
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
public class ReportDatasetRepositoryImpl implements ReportDatasetRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ReportDatasetDOMapper datasetDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listDatasets() {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("create_time DESC, id DESC");
        // 返回已经完成封装的业务结果。
        return datasetDOMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::toDatasetRecord)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord saveDataset(ReportDatasetRequest request) {
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 写入当前对象字段，保证后续持久化或响应组装的数据完整。
        ReportDatasetDO record = findDatasetById(longValue(request.getId(), -1L));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (record == null) {
            // 创建数据库记录对象，承载即将写入的业务字段。
            record = new ReportDatasetDO();
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            record.setTenantId(tenantId());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            record.setCreateTime(now);
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            applyDataset(record, request);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            record.setUpdateTime(now);
            // 将当前业务变更写入持久化层，保持数据状态同步。
            datasetDOMapper.insertSelective(record);
            // 返回已经完成封装的业务结果。
            return toDatasetRecord(record);
        }
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        applyDataset(record, request);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        datasetDOMapper.updateByPrimaryKeySelective(record);
        // 返回已经完成封装的业务结果。
        return toDatasetRecord(record);
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord getDataset(String id) {
        // 写入当前对象字段，保证后续持久化或响应组装的数据完整。
        ReportDatasetDO record = findDatasetById(longValue(id, -1L));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (record != null) {
            // 返回已经完成封装的业务结果。
            return toDatasetRecord(record);
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        ReportDatasetDOExample example = new ReportDatasetDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andDatasetKeyEqualTo(id);
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        List<ReportDatasetDO> datasets = datasetDOMapper.selectByExample(example);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (datasets.isEmpty()) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Report dataset not found: " + id);
        }
        // 返回已经完成封装的业务结果。
        return toDatasetRecord(datasets.get(0));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param record 应用层业务记录。
     * @param request 公众号管理请求参数。
     */
    private void applyDataset(ReportDatasetDO record, ReportDatasetRequest request) {
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setName(defaultText(request.getName(), defaultText(record.getName(), "Dataset")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setDatasetKey(defaultText(request.getDatasetKey(), defaultText(record.getDatasetKey(), "")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setQuerySql(defaultText(request.getQuerySql(), defaultText(record.getQuerySql(), "")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setParamsJson(request.getParams() == null ? defaultJson(record.getParamsJson()) : jsonValue(request.getParams()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private ReportDatasetDO findDatasetById(long id) {
        if (id <= 0) {
            return null;
        }
        ReportDatasetDO record = datasetDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord toDatasetRecord(ReportDatasetDO entity) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("id", String.valueOf(entity.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("name", entity.getName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("datasetKey", entity.getDatasetKey());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("status", entity.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("querySql", entity.getQuerySql());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("params", readJson(entity.getParamsJson()));
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
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
    private long longValue(Object raw, long defaultValue) {
        // 根据当前业务条件选择对应处理路径。
        if (raw instanceof Number) {
            // 返回已经完成封装的业务结果。
            return ((Number) raw).longValue();
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return defaultValue;
        }
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
