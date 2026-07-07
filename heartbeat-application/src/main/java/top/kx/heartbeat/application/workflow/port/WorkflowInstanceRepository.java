package top.kx.heartbeat.application.workflow.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.workflow.request.WorkflowStartRequest;

import java.util.List;

public interface WorkflowInstanceRepository {

    DomainRecord startInstance(String definitionId, WorkflowStartRequest request);

    List<DomainRecord> listInstances();
}
