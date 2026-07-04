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
        int pageNum = param.getPageNum() == null ? 1 : param.getPageNum();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        FlowRepository.Page<FlowDefinition> pageInfo = flowRepository.pageByQuery(
                param.getNameLike(),
                param.getCodeEqual(),
                param.getStatusEqual(),
                param.getOrderByColumn(),
                param.getOrderByDirection(),
                pageNum,
                pageSize);
        return PageResultVO.of(pageNum, pageSize, pageInfo.getTotal(),
                structMapper.toVoList(pageInfo.getRecords()));
    }
}