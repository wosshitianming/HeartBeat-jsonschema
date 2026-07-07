package top.kx.heartbeat.application.flow;

import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.common.vo.PageResultVO;
import top.kx.heartbeat.application.flow.param.FlowDefinitionQueryParam;
import top.kx.heartbeat.application.flow.vo.FlowDefinitionVO;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.repository.FlowRepository;

import javax.annotation.Resource;

/**
 * 流程定义查询应用服务（应用层）
 * <p>
 * 分页参数从 {@link FlowDefinitionQueryParam} 传入，领域仓储完成查询，
 * 由 {@link FlowAppStructMapper} 自动完成 Domain → VO 转换。
 * </p>
 *
 * @author heartbeat-team
 */
@Service
public class FlowDefinitionQueryService {

    @Resource
    private FlowRepository flowRepository;

    @Resource
    private FlowAppStructMapper structMapper;

    /**
     * 分页查询流程定义
     */
    public PageResultVO<FlowDefinitionVO> page(FlowDefinitionQueryParam param) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        int pageNum = param.getPageNum() == null ? 1 : param.getPageNum();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        FlowRepository.Page<FlowDefinition> pageInfo = flowRepository.pageByQuery(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                param.getNameLike(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                param.getCodeEqual(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                param.getStatusEqual(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                param.getOrderByColumn(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                param.getOrderByDirection(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                pageNum,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                pageSize);
        // 返回已经完成封装的业务结果。
        return PageResultVO.of(pageNum, pageSize, pageInfo.getTotal(),
                // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
                structMapper.toVoList(pageInfo.getRecords()));
    }
}
