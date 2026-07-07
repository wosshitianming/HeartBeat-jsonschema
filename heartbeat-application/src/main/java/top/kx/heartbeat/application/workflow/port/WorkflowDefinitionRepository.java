package top.kx.heartbeat.application.workflow.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.request.WorkflowDefinitionRequest;

import java.util.List;

public interface WorkflowDefinitionRepository {

    DomainRecord createDefinition(WorkflowDefinitionRequest request);

    List<DomainRecord> listDefinitions();

    DomainRecord getDefinition(String id);

    DomainRecord deployDefinition(String id);
}
