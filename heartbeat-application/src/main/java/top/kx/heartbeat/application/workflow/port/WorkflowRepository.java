package top.kx.heartbeat.application.workflow.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;
import java.util.Map;

/**
 * 工作流用用网关接口
 * <p>
 * application 层通过该接口与基础设施层解耦，统一以 {@link DomainRecord} 返回。
 * </p>
 *
 * @author heartbeat-team
 */
public interface WorkflowRepository {

    /**
     * 创建流程定义
     *
     * @param command 创建参数
     * @return 创建后的流程定义
     */
    DomainRecord createDefinition(Map<String, Object> command);

    /**
     * 列出全部流程定义
     */
    List<DomainRecord> listDefinitions();

    /**
     * 查询单个流程定义
     */
    DomainRecord getDefinition(String id);

    /**
     * 部署流程定义
     */
    DomainRecord deployDefinition(String id);

    /**
     * 启动一个流程实例
     */
    DomainRecord startInstance(String definitionId, Map<String, Object> command);

    /**
     * 列出指定用户的待办任务
     *
     * @param assigneeId 处理人 ID
     */
    List<DomainRecord> listTodoTasks(String assigneeId);

    /**
     * 列出全部流程实例
     */
    List<DomainRecord> listInstances();

    /**
     * 完成一个任务
     */
    DomainRecord completeTask(String taskId, String action, String operatorId, String comment);
}
