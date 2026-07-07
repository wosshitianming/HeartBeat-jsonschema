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
 * 提供工作流 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托工作流应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/definitions")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listDefinitions() {
        return listResponse(workflowService.listDefinitions());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，并统一委托工作流应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/definitions/{id}")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<DynamicRecordResponse> getDefinition(@PathVariable String id) {
        return recordResponse(workflowService.getDefinition(id));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托工作流应用服务完成处理。
     *
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/definitions")
    @PreAuthorize("@permissionGuard.has('biz:workflow:edit')")
    @OperLog(module = "工作流", action = "创建流程定义")
    public Result<DynamicRecordResponse> createDefinition(@RequestBody WorkflowDefinitionRequest request) {
        return recordResponse(workflowService.createDefinition(request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托工作流应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @PutMapping("/definitions/{id}/deploy")
    @PreAuthorize("@permissionGuard.has('biz:workflow:deploy')")
    @OperLog(module = "工作流", action = "部署流程定义")
    public Result<DynamicRecordResponse> deploy(@PathVariable String id) {
        return recordResponse(workflowService.deployDefinition(id));
    }

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，并统一委托工作流应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/definitions/{id}/instances")
    @PreAuthorize("@permissionGuard.has('biz:workflow:start')")
    @OperLog(module = "工作流", action = "发起流程")
    public Result<DynamicRecordResponse> start(@PathVariable String id, @RequestBody WorkflowStartRequest request) {
        return recordResponse(workflowService.startInstance(id, request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托工作流应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/instances")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listInstances() {
        return listResponse(workflowService.listInstances());
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，并统一委托工作流应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/tasks/todo")
    @PreAuthorize("@permissionGuard.has('biz:workflow:todo')")
    public Result<List<DynamicRecordResponse>> todoTasks() {
        return listResponse(workflowService.listTodoTasks());
    }

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，并统一委托工作流应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/tasks/{id}/approve")
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    @OperLog(module = "工作流", action = "审批通过")
    public Result<DynamicRecordResponse> approve(@PathVariable String id,
                                                 @RequestBody(required = false) WorkflowTaskActionRequest request) {
        return recordResponse(workflowService.approve(id, request));
    }

    /**
     * 推进流程状态流转，并保持业务动作语义清晰，并统一委托工作流应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 工作流请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/tasks/{id}/reject")
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    @OperLog(module = "工作流", action = "审批驳回")
    public Result<DynamicRecordResponse> reject(@PathVariable String id,
                                                @RequestBody(required = false) WorkflowTaskActionRequest request) {
        return recordResponse(workflowService.reject(id, request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托工作流应用服务完成处理。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托工作流应用服务完成处理。
     *
     * @param record 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
