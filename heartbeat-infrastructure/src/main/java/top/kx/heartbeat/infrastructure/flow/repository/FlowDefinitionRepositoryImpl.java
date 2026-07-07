package top.kx.heartbeat.infrastructure.flow.repository;

import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowVersion;
import top.kx.heartbeat.domain.flow.repository.FlowRepository;
import top.kx.heartbeat.infrastructure.flow.convert.FlowConvert;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowDefinitionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowVersionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.flow.HbFlowVersionDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowDefinitionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.flow.HbFlowVersionDOMapper;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 流程定义仓储实现（基于 MyBatis Generator 生成的 Example/Criteria + DOMapper）
 * <p>
 * 本类作为"演示用例"展示 MBG 风格的使用：
 * <ul>
 *     <li>查询：使用 {@link HbFlowDefinitionDOExample} + {@link HbFlowDefinitionDOExample.Criteria} 拼装条件</li>
 *     <li>写：直接 {@code mapper.insert/update/delete}，由 SQL Provider 生成 SQL</li>
 *     <li>分页：使用 PageHelper 拦截 MBG mapper 的 selectByExample，自动出 count</li>
 * </ul>
 * DO ↔ Domain 转换由 {@link FlowConvert} 完成。
 * </p>
 *
 * @author heartbeat-team
 */
@Repository
public class FlowDefinitionRepositoryImpl implements FlowRepository {

    @Autowired
    private HbFlowDefinitionDOMapper definitionDOMapper;

    @Autowired
    private HbFlowVersionDOMapper versionDOMapper;

    @Autowired
    private FlowConvert convert;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询全部流程定义（按更新时间倒序）
     */
    @Override
    public List<FlowDefinition> findAll() {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        HbFlowDefinitionDOExample example = new HbFlowDefinitionDOExample();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("update_time DESC");
        // 返回已经完成封装的业务结果。
        return definitionDOMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(convert::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 按主键查询
     */
    @Override
    public Optional<FlowDefinition> findById(String id) {
        HbFlowDefinitionDO row = definitionDOMapper.selectByPrimaryKey(parseLong(id));
        return Optional.ofNullable(row).map(convert::toDomain);
    }

    /**
     * 保存草稿：不存在则插入，存在则更新
     */
    @Override
    public FlowDefinition saveDraft(FlowDefinition definition) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        HbFlowDefinitionDO row = convert.toGenDO(definition);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        HbFlowDefinitionDO exist = row.getId() == null ? null
                // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
                : definitionDOMapper.selectByPrimaryKey(row.getId());
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (exist == null) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setCreateTime(toDate(LocalDateTime.now()));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setUpdateTime(toDate(LocalDateTime.now()));
            // 将当前业务变更写入持久化层，保持数据状态同步。
            definitionDOMapper.insertSelective(row);
        } else {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setCreateTime(exist.getCreateTime());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setUpdateTime(toDate(LocalDateTime.now()));
            // 将当前业务变更写入持久化层，保持数据状态同步。
            definitionDOMapper.updateByPrimaryKeySelective(row);
        }
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        HbFlowDefinitionDO refreshed = definitionDOMapper.selectByPrimaryKey(row.getId());
        // 返回已经完成封装的业务结果。
        return convert.toDomain(refreshed);
    }

    /**
     * 查询某流程的全部版本（按版本号倒序）
     */
    @Override
    public List<FlowVersion> findVersions(String flowId) {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        HbFlowVersionDOExample example = new HbFlowVersionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andFlowIdEqualTo(parseLong(flowId));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("version_no DESC");
        // 返回已经完成封装的业务结果。
        return versionDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(convert::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 按 (flowId, versionNo) 查询单个版本
     */
    @Override
    public Optional<FlowVersion> findVersion(String flowId, int versionNo) {
        HbFlowVersionDOExample example = new HbFlowVersionDOExample();
        example.createCriteria().andFlowIdEqualTo(parseLong(flowId)).andVersionNoEqualTo(versionNo);
        List<HbFlowVersionDOWithBLOBs> list = versionDOMapper.selectByExampleWithBLOBs(example);
        return list.isEmpty() ? Optional.empty() : Optional.of(convert.toDomain(list.get(0)));
    }

    /**
     * 保存版本
     */
    @Override
    public FlowVersion saveVersion(FlowVersion version) {
        HbFlowVersionDOWithBLOBs row = convert.toGenVersionDO(version);
        versionDOMapper.insertSelective(row);
        return findVersion(version.getFlowId(), version.getVersionNo()).orElse(version);
    }

    /**
     * 更新流程版本运行时部署信息。
     */
    @Override
    public void updateVersionRuntime(FlowVersion version) {
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        jdbcTemplate.update(
                // 计算当前分支的中间结果，供后续判断或组装使用。
                "UPDATE hb_flow_version SET runtime_engine = ?, bpmn_xml = ?, bpmn_sha256 = ?, deployment_id = ?, process_definition_id = ?, process_definition_key = ?, compile_status = ?, compile_error = ?, deployed_at = ?, update_time = NOW() WHERE id = ?",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getRuntimeEngine(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getBpmnXml(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getBpmnSha256(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getDeploymentId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getProcessDefinitionId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getProcessDefinitionKey(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getCompileStatus(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getCompileError(),
                // 计算当前分支的中间结果，供后续判断或组装使用。
                version.getDeployedAt() == null ? null : Date.from(version.getDeployedAt()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseLong(version.getId())
        );
    }

    /**
     * 激活版本：更新激活版本号与状态
     */
    @Override
    public void activateVersion(String flowId, int versionNo) {
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        HbFlowDefinitionDO exist = definitionDOMapper.selectByPrimaryKey(parseLong(flowId));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (exist == null) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("流程不存在: " + flowId);
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        exist.setActiveVersionNo(versionNo);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        exist.setStatus("ONLINE");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        exist.setUpdateTime(toDate(LocalDateTime.now()));
        // 将当前业务变更写入持久化层，保持数据状态同步。
        definitionDOMapper.updateByPrimaryKeySelective(exist);
    }

    /**
     * 更新流程定义当前激活运行时部署信息。
     */
    @Override
    public void updateActiveRuntimeDeployment(String flowId,
                                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                              String runtimeEngine,
                                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                              String activeDeploymentId,
                                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                              String activeProcessDefinitionId) {
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        jdbcTemplate.update(
                // 计算当前分支的中间结果，供后续判断或组装使用。
                "UPDATE hb_flow_definition SET runtime_engine = ?, active_deployment_id = ?, active_process_definition_id = ?, update_time = NOW() WHERE id = ?",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                runtimeEngine,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                activeDeploymentId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                activeProcessDefinitionId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseLong(flowId)
        );
    }

    /**
     * 分页查询流程定义（MBG Example + PageHelper 物理分页）
     */
    @Override
    public Page<FlowDefinition> pageByQuery(String nameLike, String codeEqual, String statusEqual,
                                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                            String orderByColumn, String orderByDirection,
                                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                            int pageNum, int pageSize) {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        HbFlowDefinitionDOExample example = new HbFlowDefinitionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        HbFlowDefinitionDOExample.Criteria c = example.createCriteria();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (nameLike != null && !nameLike.isEmpty()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            c.andNameLike("%" + nameLike + "%");
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (codeEqual != null && !codeEqual.isEmpty()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            c.andCodeEqualTo(codeEqual);
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (statusEqual != null && !statusEqual.isEmpty()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            c.andStatusEqualTo(statusEqual);
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String column = orderByColumn == null || orderByColumn.isEmpty() ? "update_time" : orderByColumn;
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String direction = "asc".equalsIgnoreCase(orderByDirection) ? "ASC" : "DESC";
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause(column + " " + direction);

        // PageHelper：开启物理分页（自动 count + select by example）
        PageHelper.startPage(pageNum, pageSize);
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        List<HbFlowDefinitionDO> rows = definitionDOMapper.selectByExample(example);
        // 根据查询结果是否为空决定返回空值还是首条记录。
        long total = rows.isEmpty() ? 0L : ((com.github.pagehelper.Page<?>) rows).getTotal();
        // 使用流式转换批量映射数据，减少中间状态暴露。
        List<FlowDefinition> records = rows.stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(convert::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
        // 返回已经完成封装的业务结果。
        return new Page<>(records, total, pageNum, pageSize);
    }

    /**
     * 按编码查询唯一流程
     */
    @Override
    public Optional<FlowDefinition> findByCode(String code) {
        HbFlowDefinitionDOExample example = new HbFlowDefinitionDOExample();
        example.createCriteria().andCodeEqualTo(code);
        List<HbFlowDefinitionDO> list = definitionDOMapper.selectByExample(example);
        return list.isEmpty() ? Optional.empty() : Optional.of(convert.toDomain(list.get(0)));
    }

    /**
     * 按主键删除
     */
    @Override
    public int deleteById(String id) {
        return definitionDOMapper.deleteByPrimaryKey(parseLong(id));
    }

    /**
     * 字符串主键 → Long 解析（互联网大厂风格：DB BIGINT，领域层用 String）
     */
    private static Long parseLong(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return Long.valueOf(s);
    }

    private static Date toDate(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
}
