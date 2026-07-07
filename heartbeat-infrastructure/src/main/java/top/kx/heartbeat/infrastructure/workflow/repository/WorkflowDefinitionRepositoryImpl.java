package top.kx.heartbeat.infrastructure.workflow.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.port.WorkflowDefinitionRepository;
import top.kx.heartbeat.application.workflow.request.WorkflowDefinitionRequest;
import top.kx.heartbeat.domain.workflow.WorkflowDefinitionStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.workflow.WfProcessDefinitionDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.workflow.WfProcessDefinitionDOMapper;
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
public class WorkflowDefinitionRepositoryImpl implements WorkflowDefinitionRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private WfProcessDefinitionDOMapper definitionDOMapper;

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createDefinition(WorkflowDefinitionRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        WorkflowDefinitionRequest safeRequest = request == null ? new WorkflowDefinitionRequest() : request;
        // 计算当前分支的中间结果，供后续判断或组装使用。
        long tenantId = tenantId();
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 创建数据库记录对象，承载即将写入的业务字段。
        WfProcessDefinitionDO entity = new WfProcessDefinitionDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setTenantId(tenantId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setName(defaultText(safeRequest.getName(), "Workflow"));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setDefinitionKey(defaultText(safeRequest.getDefinitionKey(), ""));
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isBlank(entity.getDefinitionKey())) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setDefinitionKey("wf-" + System.nanoTime());
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setVersionNo(safeRequest.getVersionNo() == null ? 1 : safeRequest.getVersionNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setFormSchema(jsonValue(safeRequest.getFormSchema()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setStatus(WorkflowDefinitionStatus.DRAFT.getCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        definitionDOMapper.insertSelective(entity);
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(toDefinitionMap(entity));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listDefinitions() {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        WfProcessDefinitionDOExample example = new WfProcessDefinitionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("create_time DESC, id DESC");
        // 返回已经完成封装的业务结果。
        return definitionDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::toDefinitionMap)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(DomainRecord::of)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord getDefinition(String id) {
        return DomainRecord.of(toDefinitionMap(requireDefinition(id)));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord deployDefinition(String id) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        WfProcessDefinitionDO entity = requireDefinition(id);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setStatus(WorkflowDefinitionStatus.DEPLOYED.getCode());
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setDeployedAt(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        definitionDOMapper.updateByPrimaryKeySelective(entity);
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(toDefinitionMap(entity));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private WfProcessDefinitionDO requireDefinition(String id) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        long tenantId = tenantId();
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        WfProcessDefinitionDO entity = definitionDOMapper.selectByPrimaryKey(longValue(id, -1L));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (entity != null && entity.getTenantId().equals(tenantId)) {
            // 返回已经完成封装的业务结果。
            return entity;
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        WfProcessDefinitionDOExample example = new WfProcessDefinitionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId).andDefinitionKeyEqualTo(id);
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        List<WfProcessDefinitionDO> list = definitionDOMapper.selectByExampleWithBLOBs(example);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (list.isEmpty()) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Workflow definition not found: " + id);
        }
        // 返回已经完成封装的业务结果。
        return list.get(0);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> toDefinitionMap(WfProcessDefinitionDO entity) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("id", String.valueOf(entity.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("tenantId", String.valueOf(entity.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("name", entity.getName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("definitionKey", entity.getDefinitionKey());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("versionNo", entity.getVersionNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("formSchema", readJson(entity.getFormSchema()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("status", entity.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("deployedAt", String.valueOf(entity.getDeployedAt()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("createTime", String.valueOf(entity.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("updateTime", String.valueOf(entity.getUpdateTime()));
        // 返回已经完成封装的业务结果。
        return row;
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
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
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
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
