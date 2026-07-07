package top.kx.heartbeat.application.workflow.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 承载工作流请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
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
