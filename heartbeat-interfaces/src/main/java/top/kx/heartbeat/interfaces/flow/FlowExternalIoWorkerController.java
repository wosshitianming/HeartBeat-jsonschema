package top.kx.heartbeat.interfaces.flow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.flow.runtime.*;
import top.kx.heartbeat.interfaces.common.Result;

import javax.annotation.Resource;

/**
 * Tenant-scoped protocol used by independently deployed external I/O workers.
 */
@RestController
@RequestMapping("/api/v1/flow/io-commands")
public class FlowExternalIoWorkerController {

    @Resource
    private FlowExternalIoWorkerService workerService;

    @PostMapping("/claim")
    @PreAuthorize("@permissionGuard.has('flow:worker:execute')")
    public Result<FlowExternalIoCommandView> claim(@RequestBody FlowExternalIoClaimRequest request) {
        return Result.success(workerService.claim(request).orElse(null));
    }

    @PostMapping("/{commandId}/started")
    @PreAuthorize("@permissionGuard.has('flow:worker:execute')")
    public Result<FlowExternalIoCommandView> started(@PathVariable String commandId,
                                                     @RequestBody FlowExternalIoStartedRequest request) {
        return Result.success(workerService.markCallStarted(commandId, request));
    }

    @PostMapping("/{commandId}/lease")
    @PreAuthorize("@permissionGuard.has('flow:worker:execute')")
    public Result<FlowExternalIoCommandView> renewLease(@PathVariable String commandId,
                                                        @RequestBody FlowExternalIoStartedRequest request) {
        return Result.success(workerService.renewLease(commandId, request));
    }

    @PostMapping("/{commandId}/complete")
    @PreAuthorize("@permissionGuard.has('flow:worker:execute')")
    public Result<FlowExternalIoCommandView> complete(@PathVariable String commandId,
                                                      @RequestBody FlowExternalIoCompletionRequest request) {
        return Result.success(workerService.complete(commandId, request));
    }

    @PostMapping("/{commandId}/resolve")
    @PreAuthorize("@permissionGuard.has('flow:definition:publish')")
    public Result<FlowExternalIoCommandView> resolve(@PathVariable String commandId,
                                                     @RequestBody FlowExternalIoResolutionRequest request) {
        return Result.success(workerService.resolve(commandId, request));
    }
}
