package top.kx.heartbeat.application.workflow.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.request.WorkflowDefinitionRequest;

import java.util.List;

/**
 * 定义工作流持久化端口，隔离应用层与具体数据访问实现。
 */
public interface WorkflowDefinitionRepository {


    DomainRecord createDefinition(WorkflowDefinitionRequest request);


    List<DomainRecord> listDefinitions();


    DomainRecord getDefinition(String id);


    DomainRecord deployDefinition(String id);
}
