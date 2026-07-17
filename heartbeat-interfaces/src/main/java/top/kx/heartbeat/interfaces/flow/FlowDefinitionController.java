package top.kx.heartbeat.interfaces.flow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.kx.heartbeat.application.common.vo.PageResultVO;
import top.kx.heartbeat.application.flow.FlowDefinitionQueryService;
import top.kx.heartbeat.application.flow.param.FlowDefinitionQueryParam;
import top.kx.heartbeat.application.flow.vo.FlowDefinitionVO;
import top.kx.heartbeat.interfaces.common.Result;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 流程定义 Controller（互联网大厂分层风格）
 * <p>
 * Controller 只做四件事：接收 Param、调用 Service、返回 VO。
 * 严禁在 Controller 中出现：
 * <ul>
 *   <li>直接调用 Mapper / DAO</li>
 *   <li>手工拼装 Example 条件</li>
 *   <li>DTO 与 VO 互转的胶水代码</li>
 * </ul>
 * </p>
 *
 * @author heartbeat-team
 */
@RestController
@RequestMapping("/api/v1/flow-definitions")
public class FlowDefinitionController {

    /** 流程定义查询应用服务 */
    @Resource
    private FlowDefinitionQueryService flowDefinitionQueryService;

    /**
     * 分页查询流程定义
     *
     * @param param 查询入参
     * @return 分页结果
     */
    @PostMapping("/page")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<PageResultVO<FlowDefinitionVO>> page(@RequestBody @Valid FlowDefinitionQueryParam param) {
        PageResultVO<FlowDefinitionVO> page = flowDefinitionQueryService.page(param);
        return Result.success(page);
    }
}
