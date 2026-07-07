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

@RestController
@RequestMapping("/api/v1/flows")
public class FlowController {

    @Resource
    private FlowApplicationService flowApplicationService;

    @GetMapping
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowDefinition>> list() {
        return Result.success(flowApplicationService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowDefinition> get(@PathVariable String id) {
        return Result.success(flowApplicationService.get(id));
    }

    @PostMapping
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> create(@RequestBody FlowDefinition flow) {
        return Result.success(flowApplicationService.saveDraft(flow));
    }

    @PutMapping("/{id}/draft")
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<FlowDefinition> saveDraft(@PathVariable String id, @RequestBody FlowDefinition flow) {
        flow.setId(id);
        return Result.success(flowApplicationService.saveDraft(flow));
    }

    @PostMapping("/{id}/compile")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<DynamicRecordResponse> compile(@PathVariable String id, @RequestBody FlowDefinition flow) {
        flow.setId(id);
        RecordResponse compiled = flowApplicationService.compile(flow);
        return Result.success(DynamicRecordResponse.from(compiled));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowVersion> publish(@PathVariable String id) {
        return Result.success(flowApplicationService.publish(id));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowVersion>> versions(@PathVariable String id) {
        return Result.success(flowApplicationService.versions(id));
    }

    @PostMapping("/{id}/debug")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowDebugResult> debug(@PathVariable String id,
                                         @RequestBody(required = false) FlowInputRequest input) {
        return Result.success(flowApplicationService.debug(id, variables(input)));
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowRun> run(@PathVariable String id,
                               @RequestBody(required = false) FlowInputRequest input) {
        return Result.success(flowApplicationService.run(id, variables(input)));
    }

    @GetMapping("/{id}/runs")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRun>> runs(@PathVariable String id) {
        return Result.success(flowApplicationService.runs(id));
    }

    @GetMapping("/runs/{runId}")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<FlowRun> runDetail(@PathVariable String runId) {
        return Result.success(flowApplicationService.runDetail(runId));
    }

    @GetMapping("/runs/{runId}/events")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> runEvents(@PathVariable String runId) {
        return Result.success(flowApplicationService.runEvents(runId));
    }

    @GetMapping("/runs/{runId}/replay")
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<FlowRunEvent>> replay(@PathVariable String runId) {
        return Result.success(flowApplicationService.replay(runId));
    }

    @PostMapping("/runs/{runId}/cancel")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<Boolean> cancel(@PathVariable String runId,
                                  @RequestBody(required = false) FlowCancelRequest input) {
        String reason = input == null || input.getReason() == null || input.getReason().trim().isEmpty()
                ? "用户取消流程运行"
                : input.getReason().trim();
        flowApplicationService.cancel(runId, reason);
        return Result.success(Boolean.TRUE);
    }

    @PutMapping("/{id}/active-version/{versionNo}")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowDefinition> activate(@PathVariable String id, @PathVariable int versionNo) {
        return Result.success(flowApplicationService.activate(id, versionNo));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> variables(FlowInputRequest input) {
        Object variables = input == null ? null : input.getVariables();
        return variables instanceof Map ? (Map<String, Object>) variables : Collections.emptyMap();
    }
}
