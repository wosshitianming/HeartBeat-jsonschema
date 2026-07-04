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
        HbFlowDefinitionDOExample example = new HbFlowDefinitionDOExample();
        example.setOrderByClause("update_time DESC");
        return definitionDOMapper.selectByExample(example)
                .stream()
                .map(convert::toDomain)
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
        HbFlowDefinitionDO row = convert.toGenDO(definition);
        HbFlowDefinitionDO exist = row.getId() == null ? null
                : definitionDOMapper.selectByPrimaryKey(row.getId());
        if (exist == null) {
            row.setCreateTime(toDate(LocalDateTime.now()));
            row.setUpdateTime(toDate(LocalDateTime.now()));
            definitionDOMapper.insertSelective(row);
        } else {
            row.setCreateTime(exist.getCreateTime());
            row.setUpdateTime(toDate(LocalDateTime.now()));
            definitionDOMapper.updateByPrimaryKeySelective(row);
        }
        HbFlowDefinitionDO refreshed = definitionDOMapper.selectByPrimaryKey(row.getId());
        return convert.toDomain(refreshed);
    }

    /**
     * 查询某流程的全部版本（按版本号倒序）
     */
    @Override
    public List<FlowVersion> findVersions(String flowId) {
        HbFlowVersionDOExample example = new HbFlowVersionDOExample();
        example.createCriteria().andFlowIdEqualTo(parseLong(flowId));
        example.setOrderByClause("version_no DESC");
        return versionDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(convert::toDomain)
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
        jdbcTemplate.update(
                "UPDATE hb_flow_version SET runtime_engine = ?, bpmn_xml = ?, bpmn_sha256 = ?, deployment_id = ?, process_definition_id = ?, process_definition_key = ?, compile_status = ?, compile_error = ?, deployed_at = ?, update_time = NOW() WHERE id = ?",
                version.getRuntimeEngine(),
                version.getBpmnXml(),
                version.getBpmnSha256(),
                version.getDeploymentId(),
                version.getProcessDefinitionId(),
                version.getProcessDefinitionKey(),
                version.getCompileStatus(),
                version.getCompileError(),
                version.getDeployedAt() == null ? null : Date.from(version.getDeployedAt()),
                parseLong(version.getId())
        );
    }

    /**
     * 激活版本：更新激活版本号与状态
     */
    @Override
    public void activateVersion(String flowId, int versionNo) {
        HbFlowDefinitionDO exist = definitionDOMapper.selectByPrimaryKey(parseLong(flowId));
        if (exist == null) {
            throw new IllegalArgumentException("流程不存在: " + flowId);
        }
        exist.setActiveVersionNo(versionNo);
        exist.setStatus("ONLINE");
        exist.setUpdateTime(toDate(LocalDateTime.now()));
        definitionDOMapper.updateByPrimaryKeySelective(exist);
    }

    /**
     * 更新流程定义当前激活运行时部署信息。
     */
    @Override
    public void updateActiveRuntimeDeployment(String flowId,
                                              String runtimeEngine,
                                              String activeDeploymentId,
                                              String activeProcessDefinitionId) {
        jdbcTemplate.update(
                "UPDATE hb_flow_definition SET runtime_engine = ?, active_deployment_id = ?, active_process_definition_id = ?, update_time = NOW() WHERE id = ?",
                runtimeEngine,
                activeDeploymentId,
                activeProcessDefinitionId,
                parseLong(flowId)
        );
    }

    /**
     * 分页查询流程定义（MBG Example + PageHelper 物理分页）
     */
    @Override
    public Page<FlowDefinition> pageByQuery(String nameLike, String codeEqual, String statusEqual,
                                            String orderByColumn, String orderByDirection,
                                            int pageNum, int pageSize) {
        HbFlowDefinitionDOExample example = new HbFlowDefinitionDOExample();
        HbFlowDefinitionDOExample.Criteria c = example.createCriteria();
        if (nameLike != null && !nameLike.isEmpty()) {
            c.andNameLike("%" + nameLike + "%");
        }
        if (codeEqual != null && !codeEqual.isEmpty()) {
            c.andCodeEqualTo(codeEqual);
        }
        if (statusEqual != null && !statusEqual.isEmpty()) {
            c.andStatusEqualTo(statusEqual);
        }
        String column = orderByColumn == null || orderByColumn.isEmpty() ? "update_time" : orderByColumn;
        String direction = "asc".equalsIgnoreCase(orderByDirection) ? "ASC" : "DESC";
        example.setOrderByClause(column + " " + direction);

        // PageHelper：开启物理分页（自动 count + select by example）
        PageHelper.startPage(pageNum, pageSize);
        List<HbFlowDefinitionDO> rows = definitionDOMapper.selectByExample(example);
        long total = rows.isEmpty() ? 0L : ((com.github.pagehelper.Page<?>) rows).getTotal();
        List<FlowDefinition> records = rows.stream()
                .map(convert::toDomain)
                .collect(Collectors.toList());
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
