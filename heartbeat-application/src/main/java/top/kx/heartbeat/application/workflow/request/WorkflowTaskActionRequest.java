package top.kx.heartbeat.application.workflow.request;

import lombok.Data;

/**
 * 承载工作流请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class WorkflowTaskActionRequest {

    private String comment;
}
