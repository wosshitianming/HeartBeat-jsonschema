// 注释：声明当前文件所属的包路径。
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

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@RestController
// 注释：声明当前元素使用的注解配置。
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private WorkflowService workflowService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/definitions")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listDefinitions() {
        // 注释：返回当前处理结果。
        return listResponse(workflowService.listDefinitions());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/definitions/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<DynamicRecordResponse> getDefinition(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return recordResponse(workflowService.getDefinition(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/definitions")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "工作流", action = "创建流程定义")
    public Result<DynamicRecordResponse> createDefinition(@RequestBody WorkflowDefinitionRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(workflowService.createDefinition(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/definitions/{id}/deploy")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:deploy')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "工作流", action = "部署流程定义")
    public Result<DynamicRecordResponse> deploy(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return recordResponse(workflowService.deployDefinition(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/definitions/{id}/instances")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:start')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "工作流", action = "发起流程")
    public Result<DynamicRecordResponse> start(@PathVariable String id, @RequestBody WorkflowStartRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(workflowService.startInstance(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/instances")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listInstances() {
        // 注释：返回当前处理结果。
        return listResponse(workflowService.listInstances());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/tasks/todo")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:todo')")
    public Result<List<DynamicRecordResponse>> todoTasks() {
        // 注释：返回当前处理结果。
        return listResponse(workflowService.listTodoTasks());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/tasks/{id}/approve")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "工作流", action = "审批通过")
    public Result<DynamicRecordResponse> approve(@PathVariable String id,
                                                 // 注释：声明当前元素使用的注解配置。
                                                 @RequestBody(required = false) WorkflowTaskActionRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(workflowService.approve(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/tasks/{id}/reject")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "工作流", action = "审批驳回")
    public Result<DynamicRecordResponse> reject(@PathVariable String id,
                                                // 注释：声明当前元素使用的注解配置。
                                                @RequestBody(required = false) WorkflowTaskActionRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(workflowService.reject(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.fromRecordList(records));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.from(record));
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
