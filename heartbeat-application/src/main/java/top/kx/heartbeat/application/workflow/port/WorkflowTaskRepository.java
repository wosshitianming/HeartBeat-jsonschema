package top.kx.heartbeat.application.workflow.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;

/**
 * 定义工作流持久化端口，隔离应用层与具体数据访问实现。
 */
public interface WorkflowTaskRepository {


    List<DomainRecord> listTodoTasks(String assigneeId);


    DomainRecord completeTask(String taskId, String action, String operatorId, String comment);
}
