package top.kx.heartbeat.interfaces.flow;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.common.vo.PageResultVO;
import top.kx.heartbeat.application.flow.FlowApplicationService;
import top.kx.heartbeat.application.flow.FlowRunOperationsService;
import top.kx.heartbeat.application.flow.param.FlowRunQueryParam;
import top.kx.heartbeat.application.flow.param.FlowRunSummaryParam;
import top.kx.heartbeat.application.flow.request.FlowCancelRequest;
import top.kx.heartbeat.application.flow.request.FlowInputRequest;
import top.kx.heartbeat.application.flow.request.FlowRetryRequest;
import top.kx.heartbeat.application.flow.runtime.FlowDebugResult;
import top.kx.heartbeat.application.flow.vo.FlowOperationsSummaryVO;
import top.kx.heartbeat.application.flow.vo.FlowRunListItemVO;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.model.FlowVersion;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 提供流程执行 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/flows")
public class FlowController {

    @Resource
    private FlowApplicationService flowApplicationService;

    @Resource
    private FlowRunOperationsService flowRunOperationsService;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托流程执行应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowDefinition>> list() {
        return Result.success(flowApplicationService.list());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowDefinition> get(@PathVariable String id) {
        return Result.success(flowApplicationService.get(id));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托流程执行应用服务完成处理。
     *
     * @param flow 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PostMapping
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> create(@RequestBody FlowDefinition flow) {
        return Result.success(flowApplicationService.saveDraft(flow));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param flow 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/{id}/draft")
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> saveDraft(@PathVariable String id, @RequestBody FlowDefinition flow) {
        flow.setId(id);
        return Result.success(flowApplicationService.saveDraft(flow));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param flow 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/{id}/compile")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<DynamicRecordResponse> compile(@PathVariable String id, @RequestBody FlowDefinition flow) {
        flow.setId(id);
        RecordResponse compiled = flowApplicationService.compile(flow);
        return Result.success(DynamicRecordResponse.from(compiled));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowVersion> publish(@PathVariable String id) {
        return Result.success(flowApplicationService.publish(id));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowVersion>> versions(@PathVariable String id) {
        return Result.success(flowApplicationService.versions(id));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param input 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/{id}/debug")
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDebugResult> debug(@PathVariable String id,
                                         @RequestBody(required = false) FlowInputRequest input) {
        return Result.success(flowApplicationService.debug(id, variables(input)));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param input 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/{id}/run")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowRun> run(@PathVariable String id,
                               @RequestBody(required = false) FlowInputRequest input) {
        return Result.success(flowApplicationService.run(id, variables(input)));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/{id}/runs")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRun>> runs(@PathVariable String id) {
        return Result.success(flowApplicationService.runs(id));
    }

    @PostMapping("/runs/page")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<PageResultVO<FlowRunListItemVO>> runPage(@RequestBody(required = false) FlowRunQueryParam param) {
        return Result.success(flowRunOperationsService.page(param));
    }

    @PostMapping("/runs/summary")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowOperationsSummaryVO> runSummary(@RequestBody(required = false) FlowRunSummaryParam param) {
        return Result.success(flowRunOperationsService.summary(param));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param runId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/runs/{runId}")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowRun> runDetail(@PathVariable String runId) {
        return Result.success(flowApplicationService.runDetail(runId));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param runId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/runs/{runId}/events")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> runEvents(@PathVariable String runId) {
        return Result.success(flowApplicationService.runEvents(runId));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param runId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/runs/{runId}/replay")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> replay(@PathVariable String runId) {
        return Result.success(flowApplicationService.replay(runId));
    }

    @PostMapping("/runs/{runId}/retry")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowRun> retry(@PathVariable String runId,
                                 @RequestBody(required = false) FlowRetryRequest request) {
        return Result.success(flowRunOperationsService.retry(runId, request));
    }

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，并统一委托流程执行应用服务完成处理。
     *
     * @param runId 业务记录标识。
     * @param input 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/runs/{runId}/cancel")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<Boolean> cancel(@PathVariable String runId,
                                  @RequestBody(required = false) FlowCancelRequest input) {
        String reason = input == null || StringUtils.isBlank(input.getReason())
                ? "用户取消流程运行"
                : input.getReason().trim();
        flowApplicationService.cancel(runId, reason);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param versionNo 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/{id}/active-version/{versionNo}")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowDefinition> activate(@PathVariable String id, @PathVariable int versionNo) {
        return Result.success(flowApplicationService.activate(id, versionNo));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowDefinition> deactivate(@PathVariable String id) {
        return Result.success(flowApplicationService.deactivate(id));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托流程执行应用服务完成处理。
     *
     * @param input 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private Map<String, Object> variables(FlowInputRequest input) {
        return input == null || input.getVariables() == null ? Collections.emptyMap() : input.getVariables();
    }
}
