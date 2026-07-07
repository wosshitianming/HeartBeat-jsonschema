// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.interfaces.flow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.flow.FlowApplicationService;
import top.kx.heartbeat.application.flow.request.FlowCancelRequest;
import top.kx.heartbeat.application.flow.request.FlowInputRequest;
import top.kx.heartbeat.application.flow.runtime.FlowDebugResult;
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@RestController
// 注释：声明当前元素使用的注解配置。
@RequestMapping("/api/v1/flows")
public class FlowController {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private FlowApplicationService flowApplicationService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowDefinition>> list() {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.list());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowDefinition> get(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.get(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> create(@RequestBody FlowDefinition flow) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.saveDraft(flow));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/{id}/draft")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> saveDraft(@PathVariable String id, @RequestBody FlowDefinition flow) {
        // 注释：执行当前代码行。
        flow.setId(id);
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.saveDraft(flow));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/{id}/compile")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<DynamicRecordResponse> compile(@PathVariable String id, @RequestBody FlowDefinition flow) {
        // 注释：执行当前代码行。
        flow.setId(id);
        // 注释：设置或计算当前变量值。
        RecordResponse compiled = flowApplicationService.compile(flow);
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.from(compiled));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/{id}/publish")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowVersion> publish(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.publish(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/{id}/versions")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowVersion>> versions(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.versions(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/{id}/debug")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowDebugResult> debug(@PathVariable String id,
                                         // 注释：声明当前元素使用的注解配置。
                                         @RequestBody(required = false) FlowInputRequest input) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.debug(id, variables(input)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/{id}/run")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowRun> run(@PathVariable String id,
                               // 注释：声明当前元素使用的注解配置。
                               @RequestBody(required = false) FlowInputRequest input) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.run(id, variables(input)));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/{id}/runs")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRun>> runs(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.runs(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/runs/{runId}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowRun> runDetail(@PathVariable String runId) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.runDetail(runId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/runs/{runId}/events")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> runEvents(@PathVariable String runId) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.runEvents(runId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/runs/{runId}/replay")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> replay(@PathVariable String runId) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.replay(runId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/runs/{runId}/cancel")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<Boolean> cancel(@PathVariable String runId,
                                  // 注释：声明当前元素使用的注解配置。
                                  @RequestBody(required = false) FlowCancelRequest input) {
        // 注释：设置或计算当前变量值。
        String reason = input == null || input.getReason() == null || input.getReason().trim().isEmpty()
                // 注释：执行当前代码行。
                ? "用户取消流程运行"
                // 注释：执行当前代码行。
                : input.getReason().trim();
        // 注释：执行当前代码行。
        flowApplicationService.cancel(runId, reason);
        // 注释：返回当前处理结果。
        return Result.success(Boolean.TRUE);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/{id}/active-version/{versionNo}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowDefinition> activate(@PathVariable String id, @PathVariable int versionNo) {
        // 注释：返回当前处理结果。
        return Result.success(flowApplicationService.activate(id, versionNo));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @SuppressWarnings("unchecked")
    private Map<String, Object> variables(FlowInputRequest input) {
        // 注释：设置或计算当前变量值。
        Object variables = input == null ? null : input.getVariables();
        // 注释：返回当前处理结果。
        return variables instanceof Map ? (Map<String, Object>) variables : Collections.emptyMap();
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
