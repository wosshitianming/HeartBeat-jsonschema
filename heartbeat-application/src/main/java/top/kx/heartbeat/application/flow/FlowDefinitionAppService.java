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
        FlowDefinition domain = (param.getId() == null || param.getId().isEmpty())
                ? structMapper.toDomain(param)
                : flowRepository.findById(param.getId())
                        .map(existing -> mergeFromParam(existing, param))
                        .orElseGet(() -> structMapper.toDomain(param));
        return structMapper.toVo(flowRepository.saveDraft(domain));
    }

    /**
     * 把 Param 的可空字段合并进已有 domain（保留 createTime/variables 等不变）
     */
    private FlowDefinition mergeFromParam(FlowDefinition existing, FlowDefinitionSaveParam param) {
        if (param.getName() != null) {
            existing.setName(param.getName());
        }
        if (param.getCode() != null) {
            existing.setCode(param.getCode());
        }
        if (param.getDescription() != null) {
            existing.setDescription(param.getDescription());
        }
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