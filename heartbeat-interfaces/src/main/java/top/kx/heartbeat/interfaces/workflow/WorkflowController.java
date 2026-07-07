package top.kx.heartbeat.interfaces.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.workflow.WorkflowService;
import top.kx.heartbeat.application.workflow.request.WorkflowDefinitionRequest;
import top.kx.heartbeat.application.workflow.request.WorkflowStartRequest;
import top.kx.heartbeat.application.workflow.request.WorkflowTaskActionRequest;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @GetMapping("/definitions")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listDefinitions() {
        return listResponse(workflowService.listDefinitions());
    }

    @GetMapping("/definitions/{id}")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<DynamicRecordResponse> getDefinition(@PathVariable String id) {
        return recordResponse(workflowService.getDefinition(id));
    }

    @PostMapping("/definitions")
    @PreAuthorize("@permissionGuard.has('biz:workflow:edit')")
    @OperLog(module = "工作流", action = "创建流程定义")
    public Result<DynamicRecordResponse> createDefinition(@RequestBody WorkflowDefinitionRequest request) {
        return recordResponse(workflowService.createDefinition(request));
    }

    @PutMapping("/definitions/{id}/deploy")
    @PreAuthorize("@permissionGuard.has('biz:workflow:deploy')")
    @OperLog(module = "工作流", action = "部署流程定义")
    public Result<DynamicRecordResponse> deploy(@PathVariable String id) {
        return recordResponse(workflowService.deployDefinition(id));
    }

    @PostMapping("/definitions/{id}/instances")
    @PreAuthorize("@permissionGuard.has('biz:workflow:start')")
    @OperLog(module = "工作流", action = "发起流程")
    public Result<DynamicRecordResponse> start(@PathVariable String id, @RequestBody WorkflowStartRequest request) {
        return recordResponse(workflowService.startInstance(id, request));
    }

    @GetMapping("/instances")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listInstances() {
        return listResponse(workflowService.listInstances());
    }

    @GetMapping("/tasks/todo")
    @PreAuthorize("@permissionGuard.has('biz:workflow:todo')")
    public Result<List<DynamicRecordResponse>> todoTasks() {
        return listResponse(workflowService.listTodoTasks());
    }

    @PostMapping("/tasks/{id}/approve")
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    @OperLog(module = "工作流", action = "审批通过")
    public Result<DynamicRecordResponse> approve(@PathVariable String id,
                                                 @RequestBody(required = false) WorkflowTaskActionRequest request) {
        return recordResponse(workflowService.approve(id, request));
    }

    @PostMapping("/tasks/{id}/reject")
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    @OperLog(module = "工作流", action = "审批驳回")
    public Result<DynamicRecordResponse> reject(@PathVariable String id,
                                                @RequestBody(required = false) WorkflowTaskActionRequest request) {
        return recordResponse(workflowService.reject(id, request));
    }

    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
