package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable 人工任务桥接服务。
 *
 * <p>用于把 Flowable UserTask 查询和完成动作包装为 HeartBeat 任务入口。</p>
 */
@Service
public class FlowableTaskBridge {

    /**
     * Flowable 任务服务。
     */
    @Resource
    private TaskService taskService;

    /**
     * 查询用户待办任务。
     *
     * @param assignee 处理人标识
     * @return 待办任务列表
     */
    public List<Map<String, Object>> todo(String assignee) {
        // 查询 Flowable 待办任务。
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();
        // 创建响应列表。
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历待办任务。
        for (Task task : tasks) {
            // 创建任务摘要。
            Map<String, Object> item = new LinkedHashMap<>();
            // 写入任务标识。
            item.put("taskId", task.getId());
            // 写入任务名称。
            item.put("name", task.getName());
            // 写入处理人。
            item.put("assignee", task.getAssignee());
            // 写入流程实例标识。
            item.put("processInstanceId", task.getProcessInstanceId());
            // 写入创建时间。
            item.put("createTime", task.getCreateTime());
            // 添加任务摘要。
            result.add(item);
        }
        // 返回待办任务列表。
        return result;
    }

    /**
     * 完成人工任务。
     *
     * @param taskId 任务标识
     * @param variables 任务变量
     */
    public void complete(String taskId, Map<String, Object> variables) {
        // 完成 Flowable 任务。
        taskService.complete(taskId, variables);
    }

    /**
     * 拒绝人工任务。
     *
     * @param taskId 任务标识
     * @param reason 拒绝原因
     */
    public void reject(String taskId, String reason) {
        // 创建拒绝变量。
        Map<String, Object> variables = new LinkedHashMap<>();
        // 写入拒绝标记。
        variables.put("approved", false);
        // 写入拒绝原因。
        variables.put("rejectReason", reason);
        // 完成任务并交给 BPMN 条件分支处理。
        taskService.complete(taskId, variables);
    }
}
