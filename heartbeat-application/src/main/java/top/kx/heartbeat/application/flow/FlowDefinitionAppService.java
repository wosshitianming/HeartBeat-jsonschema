package top.kx.heartbeat.application.flow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.flow.param.FlowDefinitionSaveParam;
import top.kx.heartbeat.application.flow.vo.FlowDefinitionVO;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.repository.FlowRepository;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * 流程定义应用服务（Application Service）
 * <p>
 * 用例编排层：接收接口层 Param，调用领域仓储完成业务动作，由 {@link FlowAppStructMapper} 完成对象转换。
 * </p>
 *
 * @author heartbeat-team
 */
@Service
public class FlowDefinitionAppService {

    @Resource
    private FlowRepository flowRepository;

    @Resource
    private FlowAppStructMapper structMapper;

    /**
     * 查询单个流程定义
     */
    public Optional<FlowDefinitionVO> get(String id) {
        return flowRepository.findById(id).map(structMapper::toVo);
    }

    /**
     * 新增 / 更新流程定义
     */
    @Transactional
    public FlowDefinitionVO save(FlowDefinitionSaveParam param) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        FlowDefinition domain = (param.getId() == null || param.getId().isEmpty())
                // 条件成立时使用前一个分支计算出的业务值。
                ? structMapper.toDomain(param)
                // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
                : flowRepository.findById(param.getId())
                // 使用流式转换批量映射数据，减少中间状态暴露。
                        .map(existing -> mergeFromParam(existing, param))
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                        .orElseGet(() -> structMapper.toDomain(param));
        // 返回已经完成封装的业务结果。
        return structMapper.toVo(flowRepository.saveDraft(domain));
    }

    /**
     * 把 Param 的可空字段合并进已有 domain（保留 createTime/variables 等不变）
     */
    private FlowDefinition mergeFromParam(FlowDefinition existing, FlowDefinitionSaveParam param) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (param.getName() != null) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            existing.setName(param.getName());
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (param.getCode() != null) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            existing.setCode(param.getCode());
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (param.getDescription() != null) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            existing.setDescription(param.getDescription());
        }
        // 返回已经完成封装的业务结果。
        return existing;
    }

    /**
     * 删除流程定义
     */
    @Transactional
    public int delete(String id) {
        return flowRepository.deleteById(id);
    }
}
