package top.kx.heartbeat.interfaces.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.workflow.WorkflowService;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.request.DynamicRecordRequest;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 工作流接口控制器。
 *
 * <p>负责流程定义、流程实例和待办任务的 HTTP 协议适配。</p>
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    /**
     * 工作流应用服务。
     */
    @Resource
    private WorkflowService workflowService;

    /**
     * 查询流程定义列表。
     *
     * @return 流程定义列表响应
     */
    @GetMapping("/definitions")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listDefinitions() {
        // 查询流程定义动态记录列表。
        List<Map<String, Object>> definitions = workflowService.listDefinitions();
        // 转换为统一动态响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(definitions);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询流程定义详情。
     *
     * @param id 流程定义标识
     * @return 流程定义详情响应
     */
    @GetMapping("/definitions/{id}")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<DynamicRecordResponse> getDefinition(@PathVariable String id) {
        // 查询流程定义动态记录。
        Map<String, Object> definition = workflowService.getDefinition(id);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(definition);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 创建流程定义。
     *
     * @param request 流程定义创建参数
     * @return 流程定义创建结果响应
     */
    @PostMapping("/definitions")
    @PreAuthorize("@permissionGuard.has('biz:workflow:edit')")
    @OperLog(module = "工作流", action = "创建流程定义")
    public Result<DynamicRecordResponse> createDefinition(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 创建流程定义动态记录。
        Map<String, Object> definition = workflowService.createDefinition(payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(definition);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 部署流程定义。
     *
     * @param id 流程定义标识
     * @return 流程定义部署结果响应
     */
    @PutMapping("/definitions/{id}/deploy")
    @PreAuthorize("@permissionGuard.has('biz:workflow:deploy')")
    @OperLog(module = "工作流", action = "部署流程定义")
    public Result<DynamicRecordResponse> deploy(@PathVariable String id) {
        // 部署流程定义动态记录。
        Map<String, Object> definition = workflowService.deployDefinition(id);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(definition);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 发起流程实例。
     *
     * @param id 流程定义标识
     * @param request 流程实例启动参数
     * @return 流程实例启动结果响应
     */
    @PostMapping("/definitions/{id}/instances")
    @PreAuthorize("@permissionGuard.has('biz:workflow:start')")
    @OperLog(module = "工作流", action = "发起流程")
    public Result<DynamicRecordResponse> start(@PathVariable String id, @RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 启动流程实例动态记录。
        Map<String, Object> instance = workflowService.startInstance(id, payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(instance);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询流程实例列表。
     *
     * @return 流程实例列表响应
     */
    @GetMapping("/instances")
    @PreAuthorize("@permissionGuard.has('biz:workflow:list')")
    public Result<List<DynamicRecordResponse>> listInstances() {
        // 查询流程实例动态记录列表。
        List<Map<String, Object>> instances = workflowService.listInstances();
        // 转换为统一动态响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(instances);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询当前用户待办任务。
     *
     * @return 待办任务列表响应
     */
    @GetMapping("/tasks/todo")
    @PreAuthorize("@permissionGuard.has('biz:workflow:todo')")
    public Result<List<DynamicRecordResponse>> todoTasks() {
        // 查询待办任务动态记录列表。
        List<Map<String, Object>> tasks = workflowService.listTodoTasks();
        // 转换为统一动态响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(tasks);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 审批通过任务。
     *
     * @param id 工作流任务标识
     * @param request 审批参数
     * @return 审批结果响应
     */
    @PostMapping("/tasks/{id}/approve")
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    @OperLog(module = "工作流", action = "审批通过")
    public Result<DynamicRecordResponse> approve(@PathVariable String id, @RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 审批通过任务动态记录。
        Map<String, Object> task = workflowService.approve(id, payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(task);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 审批拒绝任务。
     *
     * @param id 工作流任务标识
     * @param request 审批参数
     * @return 审批结果响应
     */
    @PostMapping("/tasks/{id}/reject")
    @PreAuthorize("@permissionGuard.has('biz:workflow:approve')")
    @OperLog(module = "工作流", action = "审批驳回")
    public Result<DynamicRecordResponse> reject(@PathVariable String id, @RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 审批拒绝任务动态记录。
        Map<String, Object> task = workflowService.reject(id, payload);
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(task);
        // 返回统一接口响应。
        return Result.success(response);
    }
}
