package top.kx.heartbeat.interfaces.flow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.flow.FlowApplicationService;
import top.kx.heartbeat.application.flow.runtime.FlowDebugResult;
import top.kx.heartbeat.domain.flow.model.FlowDefinition;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowRunEvent;
import top.kx.heartbeat.domain.flow.model.FlowVersion;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.request.DynamicRecordRequest;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 可视化流程接口控制器。
 *
 * <p>负责流程定义、草稿、编译、发布、调试和运行记录查询入口。</p>
 */
@RestController
@RequestMapping("/api/v1/flows")
public class FlowController {

    /**
     * 流程应用服务。
     */
    @Resource
    private FlowApplicationService flowApplicationService;

    /**
     * 查询流程定义列表。
     *
     * @return 流程定义列表响应
     */
    @GetMapping
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowDefinition>> list() {
        // 查询流程定义列表。
        List<FlowDefinition> flows = flowApplicationService.list();
        // 返回流程定义列表。
        return Result.success(flows);
    }

    /**
     * 查询流程定义详情。
     *
     * @param id 流程定义标识
     * @return 流程定义详情响应
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowDefinition> get(@PathVariable String id) {
        // 查询流程定义详情。
        FlowDefinition flow = flowApplicationService.get(id);
        // 返回流程定义详情。
        return Result.success(flow);
    }

    /**
     * 创建流程定义草稿。
     *
     * @param flow 流程定义草稿
     * @return 流程定义保存结果
     */
    @PostMapping
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> create(@RequestBody FlowDefinition flow) {
        // 保存流程定义草稿。
        FlowDefinition saved = flowApplicationService.saveDraft(flow);
        // 返回流程定义保存结果。
        return Result.success(saved);
    }

    /**
     * 保存流程定义草稿。
     *
     * @param id 流程定义标识
     * @param flow 流程定义草稿
     * @return 流程定义保存结果
     */
    @PutMapping("/{id}/draft")
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> saveDraft(@PathVariable String id,
                                            @RequestBody FlowDefinition flow) {
        // 使用路径标识覆盖请求体标识。
        flow.setId(id);
        // 保存流程定义草稿。
        FlowDefinition saved = flowApplicationService.saveDraft(flow);
        // 返回流程定义保存结果。
        return Result.success(saved);
    }

    /**
     * 编译流程定义。
     *
     * @param id 流程定义标识
     * @param flow 流程定义草稿
     * @return 流程编译结果响应
     */
    @PostMapping("/{id}/compile")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<DynamicRecordResponse> compile(@PathVariable String id,
                                                 @RequestBody FlowDefinition flow) {
        // 使用路径标识覆盖请求体标识。
        flow.setId(id);
        // 编译流程定义并返回动态编译结果。
        Map<String, Object> compiled = flowApplicationService.compile(flow);
        // 将动态编译结果转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(compiled);
        // 返回流程编译结果。
        return Result.success(response);
    }

    /**
     * 发布流程定义。
     *
     * @param id 流程定义标识
     * @return 流程版本发布结果
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowVersion> publish(@PathVariable String id) {
        // 发布流程定义版本。
        FlowVersion version = flowApplicationService.publish(id);
        // 返回流程版本发布结果。
        return Result.success(version);
    }

    /**
     * 查询流程版本列表。
     *
     * @param id 流程定义标识
     * @return 流程版本列表响应
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowVersion>> versions(@PathVariable String id) {
        // 查询流程版本列表。
        List<FlowVersion> versions = flowApplicationService.versions(id);
        // 返回流程版本列表。
        return Result.success(versions);
    }

    /**
     * 调试流程定义。
     *
     * @param id 流程定义标识
     * @param input 调试输入参数
     * @return 流程调试结果
     */
    @PostMapping("/{id}/debug")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowDebugResult> debug(@PathVariable String id,
                                         @RequestBody DynamicRecordRequest input) {
        // 转换动态请求对象为调试输入映射。
        Map<String, Object> payload = input.toMap();
        // 调试流程定义。
        FlowDebugResult debugResult = flowApplicationService.debug(id, payload);
        // 返回流程调试结果。
        return Result.success(debugResult);
    }

    /**
     * 启动生产态流程运行。
     *
     * @param id 流程定义标识
     * @param input 运行输入参数
     * @return 流程运行记录
     */
    @PostMapping("/{id}/run")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowRun> run(@PathVariable String id,
                               @RequestBody DynamicRecordRequest input) {
        // 转换动态请求对象为运行输入映射。
        Map<String, Object> payload = input.toMap();
        // 启动生产态流程运行。
        FlowRun run = flowApplicationService.run(id, payload);
        // 返回流程运行记录。
        return Result.success(run);
    }

    /**
     * 查询流程运行记录列表。
     *
     * @param id 流程定义标识
     * @return 流程运行记录列表响应
     */
    @GetMapping("/{id}/runs")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRun>> runs(@PathVariable String id) {
        // 查询流程运行记录列表。
        List<FlowRun> runs = flowApplicationService.runs(id);
        // 返回流程运行记录列表。
        return Result.success(runs);
    }

    /**
     * 查询流程运行详情。
     *
     * @param runId 流程运行标识
     * @return 流程运行详情响应
     */
    @GetMapping("/runs/{runId}")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowRun> runDetail(@PathVariable String runId) {
        // 查询流程运行详情。
        FlowRun run = flowApplicationService.runDetail(runId);
        // 返回流程运行详情。
        return Result.success(run);
    }

    /**
     * 查询流程运行事件列表。
     *
     * @param runId 流程运行标识
     * @return 流程运行事件列表响应
     */
    @GetMapping("/runs/{runId}/events")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> runEvents(@PathVariable String runId) {
        // 查询流程运行事件列表。
        List<FlowRunEvent> events = flowApplicationService.runEvents(runId);
        // 返回流程运行事件列表。
        return Result.success(events);
    }

    /**
     * 查询流程运行回放事件。
     *
     * @param runId 流程运行标识
     * @return 流程运行回放响应
     */
    @GetMapping("/runs/{runId}/replay")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> replay(@PathVariable String runId) {
        // 查询流程运行回放事件。
        List<FlowRunEvent> events = flowApplicationService.replay(runId);
        // 返回流程运行回放事件。
        return Result.success(events);
    }

    /**
     * 取消流程运行。
     *
     * @param runId 流程运行标识
     * @param input 取消原因参数
     * @return 取消结果
     */
    @PostMapping("/runs/{runId}/cancel")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<Boolean> cancel(@PathVariable String runId,
                                  @RequestBody DynamicRecordRequest input) {
        // 读取取消原因。
        String reason = String.valueOf(input.toMap().getOrDefault("reason", "用户取消流程运行"));
        // 取消流程运行。
        flowApplicationService.cancel(runId, reason);
        // 返回取消成功。
        return Result.success(Boolean.TRUE);
    }

    /**
     * 激活流程指定版本。
     *
     * @param id 流程定义标识
     * @param versionNo 流程版本号
     * @return 流程定义激活结果
     */
    @PutMapping("/{id}/active-version/{versionNo}")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowDefinition> activate(@PathVariable String id,
                                           @PathVariable int versionNo) {
        // 激活流程指定版本。
        FlowDefinition flow = flowApplicationService.activate(id, versionNo);
        // 返回流程定义激活结果。
        return Result.success(flow);
    }
}
