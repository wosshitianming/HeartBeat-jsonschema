package top.kx.heartbeat.application.flow.request;

import lombok.Data;

/**
 * 承载流程执行请求参数，保持接口层到应用层的数据结构清晰稳定。
 */
@Data
public class FlowInputRequest {

    private Object variables;
}
