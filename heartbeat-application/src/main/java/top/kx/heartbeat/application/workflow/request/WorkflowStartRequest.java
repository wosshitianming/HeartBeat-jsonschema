package top.kx.heartbeat.application.workflow.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class WorkflowStartRequest {

    private String businessKey;
    private String title;
    private String initiatorId;
    @JsonAlias("assignee_id")
    private String assigneeId;
    @JsonAlias("approver_id")
    private String approverId;
    private Object payload;
}
