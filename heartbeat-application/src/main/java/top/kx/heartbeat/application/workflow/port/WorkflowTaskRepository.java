package top.kx.heartbeat.application.workflow.port;

import top.kx.heartbeat.application.common.model.DomainRecord;

import java.util.List;

public interface WorkflowTaskRepository {

    List<DomainRecord> listTodoTasks(String assigneeId);

    DomainRecord completeTask(String taskId, String action, String operatorId, String comment);
}
